package com.financetoy.order.dto;

import com.financetoy.order.OrderEventType;
import java.time.LocalDateTime;

public record OrderEventTimelineResponse(
        OrderEventType eventType,
        String detail,
        LocalDateTime occurredAt
) {
}
