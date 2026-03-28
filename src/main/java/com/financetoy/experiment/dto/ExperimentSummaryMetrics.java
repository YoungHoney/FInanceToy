package com.financetoy.experiment.dto;

public record ExperimentSummaryMetrics(
        int executedCount,
        int cancelledCount,
        int compensatedCount,
        int reconcileRequiredCount
) {
}
