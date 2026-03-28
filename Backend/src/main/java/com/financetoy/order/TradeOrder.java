package com.financetoy.order;

import com.financetoy.common.BaseTimeEntity;
import com.financetoy.experiment.ExperimentRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "trade_order")
public class TradeOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false, length = 100)
    private String accountId;

    @Column(nullable = false, length = 100)
    private String instrumentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderSide side;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedAmount;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FailureMode failureMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, length = 500)
    private String executionResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_run_id")
    private ExperimentRun experimentRun;

    protected TradeOrder() {
    }

    public TradeOrder(
            String orderId,
            String accountId,
            String instrumentCode,
            OrderSide side,
            int quantity,
            BigDecimal price,
            BigDecimal reservedAmount,
            String idempotencyKey,
            FailureMode failureMode,
            ExperimentRun experimentRun
    ) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.instrumentCode = instrumentCode;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.reservedAmount = reservedAmount;
        this.idempotencyKey = idempotencyKey;
        this.failureMode = failureMode;
        this.status = OrderStatus.CREATED;
        this.executionResult = "주문이 생성되었습니다.";
        this.experimentRun = experimentRun;
    }

    public void markReserved() {
        this.status = OrderStatus.RESERVED;
        this.executionResult = "주문 금액이 reserve 되었습니다.";
    }

    public void markExecuting() {
        this.status = OrderStatus.EXECUTING;
        this.executionResult = "외부 체결 연계를 시도 중입니다.";
    }

    public void markExecuted(String executionResult) {
        this.status = OrderStatus.EXECUTED;
        this.executionResult = executionResult;
    }

    public void markCancelled(String executionResult) {
        this.status = OrderStatus.CANCELLED;
        this.executionResult = executionResult;
    }

    public void markCompensated(String executionResult) {
        this.status = OrderStatus.COMPENSATED;
        this.executionResult = executionResult;
    }

    public void markReconcileRequired(String executionResult) {
        this.status = OrderStatus.RECONCILE_REQUIRED;
        this.executionResult = executionResult;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getInstrumentCode() {
        return instrumentCode;
    }

    public OrderSide getSide() {
        return side;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public FailureMode getFailureMode() {
        return failureMode;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public ExperimentRun getExperimentRun() {
        return experimentRun;
    }
}
