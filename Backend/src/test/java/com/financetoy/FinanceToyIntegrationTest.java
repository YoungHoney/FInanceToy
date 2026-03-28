package com.financetoy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetoy.common.config.DemoAccounts;
import com.financetoy.experiment.ExperimentRunRepository;
import com.financetoy.ledger.AccountBalance;
import com.financetoy.ledger.AccountBalanceRepository;
import com.financetoy.ledger.LedgerEntryRepository;
import com.financetoy.ledger.LedgerEntryType;
import com.financetoy.order.OrderEventRepository;
import com.financetoy.order.OrderEventType;
import com.financetoy.order.TradeOrderRepository;
import com.financetoy.reconciliation.ReconciliationJobRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
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
        String requestBody = orderRequest("idem-001", "NORMAL");

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
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest("timeout-001", "TIMEOUT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPENSATED"))
                .andReturn();

        String orderId = read(result).get("orderId").asText();

        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.RESERVE_CASH)).isEqualTo(1);
        assertThat(ledgerEntryRepository.countByOrderIdAndEntryType(orderId, LedgerEntryType.APPLY_COMPENSATION)).isEqualTo(1);
    }

    @Test
    void shouldIgnoreDuplicateExecutionCallback() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest("duplicate-001", "DUPLICATE_CALLBACK")))
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
                        .content(orderRequest("delayed-001", "DELAYED_CALLBACK")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECONCILE_REQUIRED"));

        mockMvc.perform(post("/api/reconciliations/run"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unresolvedCount").value(1))
                .andExpect(jsonPath("$.mismatchCount").value(1));
    }

    @Test
    void shouldStoreExperimentResultsForLaterComparison() throws Exception {
        MvcResult runResult = mockMvc.perform(post("/api/experiments/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "NORMAL",
                                  "tryCount": 2,
                                  "repeatCount": 2
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
    void shouldExposeOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/orders")));
    }

    private String orderRequest(String idempotencyKey, String mode) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", DemoAccounts.DEFAULT_ACCOUNT_ID);
        payload.put("instrumentCode", "EQ-CORE-001");
        payload.put("side", "BUY");
        payload.put("quantity", 1);
        payload.put("price", new BigDecimal("1000.00"));
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("mode", mode);
        return objectMapper.writeValueAsString(payload);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
