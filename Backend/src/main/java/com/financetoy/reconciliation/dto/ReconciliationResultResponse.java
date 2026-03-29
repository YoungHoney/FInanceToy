package com.financetoy.reconciliation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReconciliationResultResponse(
        String jobId,
        LocalDate businessDate,
        int matchedCount,
        int mismatchCount,
        int compensatedCount,
        int unresolvedCount,
        List<String> unresolvedItems,
        LocalDateTime updatedAt,
        LocalDateTime createdAt
) {
}
