package com.financetoy.order;

import com.financetoy.audit.AuditLogRepository;
import com.financetoy.audit.AuditLogService;
import com.financetoy.common.exception.NotFoundException;
import com.financetoy.execution.ExecutionDecision;
import com.financetoy.execution.ExecutionSimulator;
import com.financetoy.execution.ExternalExecutionOutcome;
import com.financetoy.experiment.ExperimentRun;
import com.financetoy.ledger.AccountBalance;
import com.financetoy.ledger.AccountBalanceRepository;
import com.financetoy.ledger.LedgerEntry;
import com.financetoy.ledger.LedgerEntryRepository;
import com.financetoy.ledger.LedgerEntryType;
import com.financetoy.order.dto.AuditLogTimelineResponse;
import com.financetoy.order.dto.LedgerEntryTimelineResponse;
import com.financetoy.order.dto.OrderCreateRequest;
import com.financetoy.order.dto.OrderEventTimelineResponse;
import com.financetoy.order.dto.OrderOperatorDetailResponse;
import com.financetoy.order.dto.OrderOperatorSummaryResponse;
import com.financetoy.order.dto.OrderResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Set<OrderStatus> PROBLEM_STATUSES = EnumSet.of(
            OrderStatus.CANCELLED,
            OrderStatus.COMPENSATED,
            OrderStatus.RECONCILE_REQUIRED
    );

    private final TradeOrderRepository tradeOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final ExecutionSimulator executionSimulator;
    private final AuditLogService auditLogService;

    public OrderService(
            TradeOrderRepository tradeOrderRepository,
            OrderEventRepository orderEventRepository,
            AccountBalanceRepository accountBalanceRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuditLogRepository auditLogRepository,
            ExecutionSimulator executionSimulator,
            AuditLogService auditLogService
    ) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditLogRepository = auditLogRepository;
        this.executionSimulator = executionSimulator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        return createOrder(request, null);
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, ExperimentRun experimentRun) {
        return tradeOrderRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::toIdempotentReplayResponse)
                .orElseGet(() -> processNewOrder(request, experimentRun));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        TradeOrder order = findOrder(orderId);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderOperatorSummaryResponse> getOrders(OrderStatus status, FailureMode failureMode, boolean problemOnly, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        return findOrders(status, failureMode).stream()
                .filter(order -> !problemOnly || isProblem(order.getStatus()))
                .limit(safeLimit)
                .map(this::toOperatorSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderOperatorDetailResponse getOrderDetail(String orderId) {
        TradeOrder order = findOrder(orderId);

        List<OrderEventTimelineResponse> orderEvents = orderEventRepository.findByOrderIdOrderByOccurredAtAsc(orderId).stream()
                .map(event -> new OrderEventTimelineResponse(
                        event.getEventType(),
                        event.getDetail(),
                        event.getOccurredAt()
                ))
                .toList();

        List<LedgerEntryTimelineResponse> ledgerEntries = ledgerEntryRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toLedgerTimeline)
                .toList();

        List<AuditLogTimelineResponse> auditLogs = auditLogRepository.findByDomainTypeAndDomainIdOrderByCreatedAtAsc("ORDER", orderId).stream()
                .map(log -> new AuditLogTimelineResponse(
                        log.getActionType(),
                        log.getDetail(),
                        log.getCreatedAt()
                ))
                .toList();

        return new OrderOperatorDetailResponse(
                toOperatorSummary(order),
                orderEvents,
                ledgerEntries,
                auditLogs
        );
    }

    private List<TradeOrder> findOrders(OrderStatus status, FailureMode failureMode) {
        if (status != null && failureMode != null) {
            return tradeOrderRepository.findByStatusAndFailureModeOrderByCreatedAtDesc(status, failureMode);
        }
        if (status != null) {
            return tradeOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        if (failureMode != null) {
            return tradeOrderRepository.findByFailureModeOrderByCreatedAtDesc(failureMode);
        }
        return tradeOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    private TradeOrder findOrder(String orderId) {
        return tradeOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다. orderId=" + orderId));
    }

    private OrderResponse toIdempotentReplayResponse(TradeOrder existingOrder) {
        auditLogService.record(
                "IDEMPOTENT_REPLAY",
                "ORDER",
                existingOrder.getOrderId(),
                "같은 idempotencyKey로 재호출되어 기존 주문을 반환했습니다."
        );
        return toResponse(existingOrder);
    }

    private OrderResponse processNewOrder(OrderCreateRequest request, ExperimentRun experimentRun) {
        AccountBalance accountBalance = accountBalanceRepository.findByExternalAccountIdForUpdate(request.accountId())
                .orElseThrow(() -> new NotFoundException("계좌를 찾을 수 없습니다. accountId=" + request.accountId()));

        TradeOrder existingOrder = tradeOrderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existingOrder != null) {
            return toIdempotentReplayResponse(existingOrder);
        }

        BigDecimal reservedAmount = request.price()
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        TradeOrder order = new TradeOrder(
                UUID.randomUUID().toString(),
                request.accountId(),
                request.instrumentCode(),
                request.side(),
                request.quantity(),
                request.price().setScale(2, RoundingMode.HALF_UP),
                reservedAmount,
                request.idempotencyKey(),
                request.mode(),
                experimentRun
        );
        tradeOrderRepository.saveAndFlush(order);
        recordEvent(order.getOrderId(), OrderEventType.ORDER_RECEIVED, "주문이 생성되었습니다.");
        auditLogService.record("ORDER_CREATED", "ORDER", order.getOrderId(), "주문 접수가 완료되었습니다.");

        accountBalance.reserve(reservedAmount);
        accountBalanceRepository.save(accountBalance);
        order.markReserved();
        tradeOrderRepository.save(order);
        recordEvent(order.getOrderId(), OrderEventType.CASH_RESERVED, "주문 금액을 reserve 했습니다.");
        recordLedger(order, accountBalance, LedgerEntryType.RESERVE_CASH, reservedAmount);
        auditLogService.record("CASH_RESERVED", "ORDER", order.getOrderId(), "주문 금액 reserve 처리 완료");

        order.markExecuting();
        tradeOrderRepository.save(order);
        recordEvent(order.getOrderId(), OrderEventType.EXECUTION_ATTEMPTED, "외부 체결 연계를 시도합니다.");

        ExternalExecutionOutcome outcome = executionSimulator.simulate(order.getFailureMode());
        applyPrimaryOutcome(order, accountBalance, reservedAmount, outcome);
        applyFollowUpCallbacks(order);

        return toResponse(order);
    }

    private void applyPrimaryOutcome(
            TradeOrder order,
            AccountBalance accountBalance,
            BigDecimal reservedAmount,
            ExternalExecutionOutcome outcome
    ) {
        if (outcome.decision() == ExecutionDecision.SUCCESS) {
            accountBalance.settleReserved(reservedAmount);
            accountBalanceRepository.save(accountBalance);
            recordLedger(order, accountBalance, LedgerEntryType.EXECUTE_BUY, reservedAmount);
            recordEvent(order.getOrderId(), OrderEventType.EXECUTION_CONFIRMED, "외부 체결이 확정되었습니다.");
            order.markExecuted(outcome.message());
            tradeOrderRepository.save(order);
            auditLogService.record("ORDER_EXECUTED", "ORDER", order.getOrderId(), "정상 체결 및 원장 반영 완료");
            return;
        }

        if (outcome.decision() == ExecutionDecision.REJECTED) {
            accountBalance.release(reservedAmount);
            accountBalanceRepository.save(accountBalance);
            recordLedger(order, accountBalance, LedgerEntryType.RELEASE_CASH, reservedAmount);
            recordEvent(order.getOrderId(), OrderEventType.EXECUTION_REJECTED, "외부 체결이 거절되었습니다.");
            recordEvent(order.getOrderId(), OrderEventType.ORDER_CANCELLED, "자동취소를 수행했습니다.");
            order.markCancelled(outcome.message());
            tradeOrderRepository.save(order);
            auditLogService.record("ORDER_CANCELLED", "ORDER", order.getOrderId(), "체결 거절로 자동취소 완료");
            return;
        }

        accountBalance.release(reservedAmount);
        accountBalanceRepository.save(accountBalance);
        recordLedger(order, accountBalance, LedgerEntryType.APPLY_COMPENSATION, reservedAmount);
        recordEvent(order.getOrderId(), OrderEventType.EXECUTION_TIMEOUT, "외부 체결 응답이 타임아웃되었습니다.");
        recordEvent(order.getOrderId(), OrderEventType.COMPENSATION_APPLIED, "보상처리를 수행했습니다.");
        order.markCompensated(outcome.message());
        tradeOrderRepository.save(order);
        auditLogService.record("ORDER_COMPENSATED", "ORDER", order.getOrderId(), "타임아웃으로 보상처리 완료");
    }

    private void applyFollowUpCallbacks(TradeOrder order) {
        if (order.getFailureMode() == FailureMode.DUPLICATE_CALLBACK) {
            recordEvent(order.getOrderId(), OrderEventType.DUPLICATE_CALLBACK_IGNORED, "중복 콜백을 무시했습니다.");
            auditLogService.record("DUPLICATE_CALLBACK_IGNORED", "ORDER", order.getOrderId(), "중복 콜백이 원장에 재반영되지 않았습니다.");
        }

        if (order.getFailureMode() == FailureMode.DELAYED_CALLBACK) {
            recordEvent(order.getOrderId(), OrderEventType.DELAYED_CALLBACK_RECEIVED, "보상처리 이후 늦은 체결 콜백이 도착했습니다.");
            order.markReconcileRequired("보상처리 이후 늦은 체결 콜백이 도착하여 수동 대사가 필요합니다.");
            tradeOrderRepository.save(order);
            auditLogService.record("DELAYED_CALLBACK_RECEIVED", "ORDER", order.getOrderId(), "대사 필요 상태로 전환되었습니다.");
        }
    }

    private void recordEvent(String orderId, OrderEventType eventType, String detail) {
        orderEventRepository.save(new OrderEvent(orderId, eventType, detail));
    }

    private void recordLedger(TradeOrder order, AccountBalance accountBalance, LedgerEntryType entryType, BigDecimal amount) {
        ledgerEntryRepository.save(new LedgerEntry(
                UUID.randomUUID().toString(),
                order.getOrderId(),
                order.getAccountId(),
                entryType,
                amount,
                accountBalance.getAvailableCash(),
                accountBalance.getReservedCash()
        ));
    }

    private LedgerEntryTimelineResponse toLedgerTimeline(LedgerEntry ledgerEntry) {
        return new LedgerEntryTimelineResponse(
                ledgerEntry.getTransactionId(),
                ledgerEntry.getEntryType(),
                ledgerEntry.getAmount(),
                ledgerEntry.getAvailableBalanceAfter(),
                ledgerEntry.getReservedBalanceAfter(),
                ledgerEntry.getCreatedAt()
        );
    }

    private OrderResponse toResponse(TradeOrder order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getStatus(),
                order.getReservedAmount(),
                order.getExecutionResult(),
                order.getUpdatedAt()
        );
    }

    private OrderOperatorSummaryResponse toOperatorSummary(TradeOrder order) {
        return new OrderOperatorSummaryResponse(
                order.getOrderId(),
                order.getAccountId(),
                order.getInstrumentCode(),
                order.getSide(),
                order.getQuantity(),
                order.getPrice(),
                order.getReservedAmount(),
                order.getIdempotencyKey(),
                order.getFailureMode(),
                order.getStatus(),
                order.getExecutionResult(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                isProblem(order.getStatus())
        );
    }

    private boolean isProblem(OrderStatus status) {
        return PROBLEM_STATUSES.contains(status);
    }
}
