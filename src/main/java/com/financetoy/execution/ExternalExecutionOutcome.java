package com.financetoy.execution;

public record ExternalExecutionOutcome(
        ExecutionDecision decision,
        boolean duplicateCallback,
        boolean delayedCallback,
        String message
) {
}
