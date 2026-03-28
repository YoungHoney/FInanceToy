package com.financetoy.order.dto;

import com.financetoy.order.FailureMode;
import com.financetoy.order.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderCreateRequest(
        @NotBlank(message = "accountId는 필수입니다.")
        String accountId,
        @NotBlank(message = "instrumentCode는 필수입니다.")
        String instrumentCode,
        @NotNull(message = "side는 필수입니다.")
        OrderSide side,
        @Positive(message = "quantity는 1 이상이어야 합니다.")
        int quantity,
        @NotNull(message = "price는 필수입니다.")
        @DecimalMin(value = "0.01", message = "price는 0.01 이상이어야 합니다.")
        BigDecimal price,
        @NotBlank(message = "idempotencyKey는 필수입니다.")
        String idempotencyKey,
        @NotNull(message = "mode는 필수입니다.")
        FailureMode mode
) {
}
