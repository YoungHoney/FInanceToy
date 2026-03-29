package com.financetoy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetoy.common.config.DemoAccounts;
import com.financetoy.common.exception.InsufficientFundsException;
import com.financetoy.experiment.ExperimentRunRepository;
import com.financetoy.ledger.AccountBalance;
import com.financetoy.ledger.AccountBalanceRepository;
import com.financetoy.ledger.LedgerEntryRepository;
import com.financetoy.ledger.LedgerEntryType;
import com.financetoy.order.FailureMode;
import com.financetoy.order.OrderEventRepository;
import com.financetoy.order.OrderEventType;
import com.financetoy.order.OrderService;
import com.financetoy.order.OrderSide;
import com.financetoy.order.TradeOrderRepository;
import com.financetoy.order.dto.OrderCreateRequest;
import com.financetoy.order.dto.OrderResponse;
import com.financetoy.reconciliation.ReconciliationJobRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceToyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ReconciliationJobRepository reconciliationJobRepository;

    @Autowired
    private ExperimentRunRepository experimentRunRepository;

    @Autowired
    private AccountBalanceRepository accountBalanceRepository;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderEventRepository.deleteAll();
        tradeOrderRepository.deleteAll();
        reconciliationJobRepository.deleteAll();
        experimentRunRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        accountBalanceRepository.deleteAll();
        accountBalanceRepository.save(new AccountBalance(
                DemoAccounts.DEFAULT_ACCOUNT_ID,
                new BigDecimal("1000000.00"),
                BigDecimal.ZERO
        ));
    }

    @Test
    void shouldReturnSameOrderWhenIdempotencyKeyRepeats() throws Exception {
        String requestBody = orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "idem-001", "NORMAL");

        MvcResult first = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstNode = read(first);
        JsonNode secondNode = read(second);
        String orderId = firstNode.get("orderId").asText();

        assertThat(secondNode.get("orderId").asText()).isEqualTo(orderId);
        assertThat(tradeOrderRepository.count()).isEqualTo(1);
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.RESERVE_CASH)).isEqualTo(1);
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.EXECUTE_BUY)).isEqualTo(1);
    }

    @Test
    void shouldCompensateWhenExecutionTimesOut() throws Exception {
        BigDecimal initialAvailableCash = accountBalanceRepository.findByExternalAccountId(DemoAccounts.DEFAULT_ACCOUNT_ID)
                .orElseThrow()
                .getAvailableCash();

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "timeout-001", "TIMEOUT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPENSATED"))
                .andReturn();

        String orderId = read(result).get("orderId").asText();
        AccountBalance balance = accountBalanceRepository.findByExternalAccountId(DemoAccounts.DEFAULT_ACCOUNT_ID).orElseThrow();

        assertThat(tradeOrderRepository.findByOrderId(orderId).orElseThrow().getStatus().name()).isEqualTo("COMPENSATED");
        assertThat(balance.getAvailableCash()).isEqualByComparingTo(initialAvailableCash);
        assertThat(balance.getReservedCash()).isEqualByComparingTo("0.00");
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.RESERVE_CASH)).isEqualTo(1);
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.APPLY_COMPENSATION)).isEqualTo(1);
        assertThat(orderEventCount(orderId, OrderEventType.COMPENSATION_APPLIED)).isEqualTo(1);
        assertThat(orderEventCount(orderId, OrderEventType.EXECUTION_TIMEOUT)).isEqualTo(1);
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.RELEASE_CASH)).isEqualTo(0);
    }

    @Test
    void shouldIgnoreDuplicateExecutionCallback() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "duplicate-001", "DUPLICATE_CALLBACK")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andReturn();

        String orderId = read(result).get("orderId").asText();

        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.EXECUTE_BUY)).isEqualTo(1);
        assertThat(orderEventRepository.existsByOrderIdAndEventType(orderId, OrderEventType.DUPLICATE_CALLBACK_IGNORED)).isTrue();
    }

    @Test
    void shouldMarkOrderForReconciliationWhenDelayedCallbackArrives() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "delayed-001", "DELAYED_CALLBACK")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECONCILE_REQUIRED"));

        mockMvc.perform(post("/api/reconciliations/run"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unresolvedCount").value(1))
                .andExpect(jsonPath("$.mismatchCount").value(1));
    }

    @Test
    void shouldOverwriteExistingReconciliationJobWhenRerunOnSameBusinessDate() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "delayed-rerun-001", "DELAYED_CALLBACK")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECONCILE_REQUIRED"));

        MvcResult firstRun = mockMvc.perform(post("/api/reconciliations/run"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.mismatchCount").value(1))
                .andExpect(jsonPath("$.unresolvedCount").value(1))
                .andReturn();

        MvcResult secondRun = mockMvc.perform(post("/api/reconciliations/run"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.mismatchCount").value(1))
                .andExpect(jsonPath("$.unresolvedCount").value(1))
                .andReturn();

        JsonNode firstNode = read(firstRun);
        JsonNode secondNode = read(secondRun);

        assertThat(secondNode.get("jobId").asText()).isEqualTo(firstNode.get("jobId").asText());
        assertThat(secondNode.get("businessDate").asText()).isEqualTo(firstNode.get("businessDate").asText());
        assertThat(reconciliationJobRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldExposeOperatorOrderFeedAndDetail() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "ops-normal-001", "NORMAL")))
                .andExpect(status().isCreated());

        MvcResult delayed = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(DemoAccounts.DEFAULT_ACCOUNT_ID, "ops-delayed-001", "DELAYED_CALLBACK")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECONCILE_REQUIRED"))
                .andReturn();

        String delayedOrderId = read(delayed).get("orderId").asText();

        mockMvc.perform(get("/api/orders")
                        .param("problemOnly", "true")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(delayedOrderId))
                .andExpect(jsonPath("$[0].problem").value(true))
                .andExpect(jsonPath("$[0].status").value("RECONCILE_REQUIRED"));

        mockMvc.perform(get("/api/orders/{orderId}/detail", delayedOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.orderId").value(delayedOrderId))
                .andExpect(jsonPath("$.order.problem").value(true))
                .andExpect(jsonPath("$.orderEvents.length()").isNumber())
                .andExpect(jsonPath("$.ledgerEntries.length()").isNumber())
                .andExpect(jsonPath("$.auditLogs.length()").isNumber())
                .andExpect(jsonPath("$.orderEvents[0].eventType").value("ORDER_RECEIVED"));
    }

    @Test
    void shouldStoreExperimentResultsForLaterComparison() throws Exception {
        MvcResult runResult = mockMvc.perform(post("/api/experiments/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"mode\": \"NORMAL\",
                                  \"tryCount\": 2,
                                  \"repeatCount\": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalOrders").value(4))
                .andExpect(jsonPath("$.summaryMetrics.executedCount").value(4))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andReturn();

        String runId = read(runResult).get("runId").asText();

        mockMvc.perform(get("/api/experiments/{runId}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.totalOrders").value(4));
    }

    @Test
    void shouldKeepSingleOrderUnderConcurrentIdempotentRequests() throws Exception {
        OrderCreateRequest request = orderRequestObject(
                DemoAccounts.DEFAULT_ACCOUNT_ID,
                "idem-concurrent-001",
                FailureMode.NORMAL,
                new BigDecimal("1000.00")
        );

        List<OrderResponse> responses = runConcurrently(20, () -> orderService.createOrder(request));

        Set<String> orderIds = responses.stream()
                .map(OrderResponse::orderId)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(orderIds).hasSize(1);
        assertThat(tradeOrderRepository.count()).isEqualTo(1);

        String orderId = orderIds.iterator().next();
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.RESERVE_CASH)).isEqualTo(1);
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.EXECUTE_BUY)).isEqualTo(1);

        AccountBalance balance = accountBalanceRepository.findByExternalAccountId(DemoAccounts.DEFAULT_ACCOUNT_ID).orElseThrow();
        assertThat(balance.getAvailableCash()).isEqualByComparingTo("999000.00");
        assertThat(balance.getReservedCash()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldPreventNegativeBalanceWhenConcurrentOrdersExceedCash() throws Exception {
        String accountId = "stress-investor";
        accountBalanceRepository.save(new AccountBalance(accountId, new BigDecimal("10000.00"), BigDecimal.ZERO));

        List<Boolean> results = runConcurrently(50, () -> {
            try {
                orderService.createOrder(orderRequestObject(
                        accountId,
                        "stress-" + java.util.UUID.randomUUID(),
                        FailureMode.NORMAL,
                        new BigDecimal("1000.00")
                ));
                return true;
            } catch (InsufficientFundsException ex) {
                return false;
            }
        });

        long successCount = results.stream().filter(Boolean::booleanValue).count();
        long failureCount = results.size() - successCount;

        AccountBalance balance = accountBalanceRepository.findByExternalAccountId(accountId).orElseThrow();

        assertThat(successCount).isEqualTo(10);
        assertThat(failureCount).isEqualTo(40);
        assertThat(tradeOrderRepository.count()).isEqualTo(10);
        assertThat(balance.getAvailableCash()).isEqualByComparingTo("0.00");
        assertThat(balance.getReservedCash()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldExposeOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/orders")));
    }

    private String orderRequest(String accountId, String idempotencyKey, String mode) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", accountId);
        payload.put("instrumentCode", "EQ-CORE-001");
        payload.put("side", "BUY");
        payload.put("quantity", 1);
        payload.put("price", new BigDecimal("1000.00"));
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("mode", mode);
        return objectMapper.writeValueAsString(payload);
    }

    private OrderCreateRequest orderRequestObject(
            String accountId,
            String idempotencyKey,
            FailureMode mode,
            BigDecimal price
    ) {
        return new OrderCreateRequest(
                accountId,
                "EQ-CORE-001",
                OrderSide.BUY,
                1,
                price,
                idempotencyKey,
                mode
        );
    }

    private <T> List<T> runConcurrently(int taskCount, Callable<T> task) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long orderEventCount(String orderId, OrderEventType eventType) {
        return orderEventRepository.findByOrderIdOrderByOccurredAtAsc(orderId).stream()
                .filter(event -> event.getEventType() == eventType)
                .count();
    }
}
