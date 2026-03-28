package com.financetoy.experiment.dto;

import com.financetoy.order.FailureMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExperimentRunRequest(
        @NotNull(message = "mode는 필수입니다.")
        FailureMode mode,
        @Min(value = 1, message = "tryCount는 1 이상이어야 합니다.")
        int tryCount,
        @Min(value = 1, message = "repeatCount는 1 이상이어야 합니다.")
        int repeatCount
) {
}
