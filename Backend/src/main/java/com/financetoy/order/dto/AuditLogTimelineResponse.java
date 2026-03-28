package com.financetoy.order.dto;

import java.time.LocalDateTime;

public record AuditLogTimelineResponse(
        String actionType,
        String detail,
        LocalDateTime occurredAt
) {
}
