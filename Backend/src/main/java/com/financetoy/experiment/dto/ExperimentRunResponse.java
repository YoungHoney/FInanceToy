package com.financetoy.experiment.dto;

import java.time.LocalDateTime;

public record ExperimentRunResponse(
        String runId,
        int totalOrders,
        ExperimentSummaryMetrics summaryMetrics,
        int failureCount,
        int compensationCount,
        LocalDateTime createdAt
) {
}
