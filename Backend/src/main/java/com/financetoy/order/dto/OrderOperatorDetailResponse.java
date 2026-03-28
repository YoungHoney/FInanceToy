package com.financetoy.order.dto;

import java.util.List;

public record OrderOperatorDetailResponse(
        OrderOperatorSummaryResponse order,
        List<OrderEventTimelineResponse> orderEvents,
        List<LedgerEntryTimelineResponse> ledgerEntries,
        List<AuditLogTimelineResponse> auditLogs
) {
}
