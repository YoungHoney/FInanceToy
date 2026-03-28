package com.financetoy.experiment;

import com.financetoy.common.BaseTimeEntity;
import com.financetoy.order.FailureMode;
import com.financetoy.order.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "experiment_run")
public class ExperimentRun extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FailureMode failureMode;

    @Column(nullable = false)
    private int tryCount;

    @Column(nullable = false)
    private int repeatCount;

    @Column(nullable = false)
    private int totalOrders;

    @Column(nullable = false)
    private int executedCount;

    @Column(nullable = false)
    private int cancelledCount;

    @Column(nullable = false)
    private int compensatedCount;

    @Column(nullable = false)
    private int reconcileRequiredCount;

    protected ExperimentRun() {
    }

    public ExperimentRun(String runId, FailureMode failureMode, int tryCount, int repeatCount) {
        this.runId = runId;
        this.failureMode = failureMode;
        this.tryCount = tryCount;
        this.repeatCount = repeatCount;
    }

    public void record(OrderStatus status) {
        totalOrders++;
        switch (status) {
            case EXECUTED -> executedCount++;
            case CANCELLED -> cancelledCount++;
            case COMPENSATED -> compensatedCount++;
            case RECONCILE_REQUIRED -> reconcileRequiredCount++;
            default -> {
            }
        }
    }

    public String getRunId() {
        return runId;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public int getExecutedCount() {
        return executedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public int getCompensatedCount() {
        return compensatedCount;
    }

    public int getReconcileRequiredCount() {
        return reconcileRequiredCount;
    }
}
