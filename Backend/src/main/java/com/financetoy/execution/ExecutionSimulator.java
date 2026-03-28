package com.financetoy.execution;

import com.financetoy.order.FailureMode;
import org.springframework.stereotype.Component;

@Component
public class ExecutionSimulator {

    public ExternalExecutionOutcome simulate(FailureMode failureMode) {
        return switch (failureMode) {
            case NORMAL -> new ExternalExecutionOutcome(
                    ExecutionDecision.SUCCESS,
                    false,
                    false,
                    "정상 체결이 완료되었습니다."
            );
            case REJECT -> new ExternalExecutionOutcome(
                    ExecutionDecision.REJECTED,
                    false,
                    false,
                    "외부 기관이 주문을 거절했습니다."
            );
            case TIMEOUT -> new ExternalExecutionOutcome(
                    ExecutionDecision.TIMED_OUT,
                    false,
                    false,
                    "외부 기관 응답이 지연되어 보상처리를 수행합니다."
            );
            case DUPLICATE_CALLBACK -> new ExternalExecutionOutcome(
                    ExecutionDecision.SUCCESS,
                    true,
                    false,
                    "정상 체결 후 동일 콜백이 중복 수신됩니다."
            );
            case DELAYED_CALLBACK -> new ExternalExecutionOutcome(
                    ExecutionDecision.TIMED_OUT,
                    false,
                    true,
                    "타임아웃 처리 후 늦은 체결 콜백이 도착합니다."
            );
        };
    }
}
