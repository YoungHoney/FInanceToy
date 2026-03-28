package com.financetoy.order.dto;

import com.financetoy.order.FailureMode;
import com.financetoy.order.OrderSide;
import com.financetoy.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderOperatorSummaryResponse(
        String orderId,
        String accountId,
        String instrumentCode,
        OrderSide side,
        int quantity,
        BigDecimal price,
        BigDecimal reservedAmount,
        String idempotencyKey,
        FailureMode failureMode,
        OrderStatus status,
        String executionResult,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt,
        boolean problem
) {
}
