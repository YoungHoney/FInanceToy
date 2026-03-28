package com.financetoy.order.dto;

import com.financetoy.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        String orderId,
        OrderStatus status,
        BigDecimal reservedAmount,
        String executionResult,
        LocalDateTime lastUpdatedAt
) {
}
