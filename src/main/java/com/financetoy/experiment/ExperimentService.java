package com.financetoy.experiment;

import com.financetoy.audit.AuditLogService;
import com.financetoy.common.config.DemoAccounts;
import com.financetoy.common.exception.NotFoundException;
import com.financetoy.experiment.dto.ExperimentRunRequest;
import com.financetoy.experiment.dto.ExperimentRunResponse;
import com.financetoy.experiment.dto.ExperimentSummaryMetrics;
import com.financetoy.order.OrderService;
import com.financetoy.order.OrderSide;
import com.financetoy.order.dto.OrderCreateRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperimentService {

    private static final String DEFAULT_INSTRUMENT_CODE = "EQ-CORE-001";

    private final ExperimentRunRepository experimentRunRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;

    public ExperimentService(
            ExperimentRunRepository experimentRunRepository,
            OrderService orderService,
            AuditLogService auditLogService
    ) {
        this.experimentRunRepository = experimentRunRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ExperimentRunResponse runExperiment(ExperimentRunRequest request) {
        ExperimentRun experimentRun = experimentRunRepository.save(
                new ExperimentRun(UUID.randomUUID().toString(), request.mode(), request.tryCount(), request.repeatCount())
        );

        for (int repeat = 0; repeat < request.repeatCount(); repeat++) {
            for (int attempt = 0; attempt < request.tryCount(); attempt++) {
                var orderResponse = orderService.createOrder(
                        new OrderCreateRequest(
                                DemoAccounts.DEFAULT_ACCOUNT_ID,
                                DEFAULT_INSTRUMENT_CODE,
                                OrderSide.BUY,
                                1,
                                new BigDecimal("1000.00"),
                                UUID.randomUUID().toString(),
                                request.mode()
                        ),
                        experimentRun
                );
                experimentRun.record(orderResponse.status());
            }
        }

        experimentRunRepository.save(experimentRun);
        auditLogService.record("EXPERIMENT_RUN_CREATED", "EXPERIMENT", experimentRun.getRunId(), "실험 실행이 완료되었습니다.");
        return toResponse(experimentRun);
    }

    @Transactional(readOnly = true)
    public ExperimentRunResponse getExperiment(String runId) {
        ExperimentRun experimentRun = experimentRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NotFoundException("실험 실행을 찾을 수 없습니다. runId=" + runId));
        return toResponse(experimentRun);
    }

    private ExperimentRunResponse toResponse(ExperimentRun experimentRun) {
        ExperimentSummaryMetrics metrics = new ExperimentSummaryMetrics(
                experimentRun.getExecutedCount(),
                experimentRun.getCancelledCount(),
                experimentRun.getCompensatedCount(),
                experimentRun.getReconcileRequiredCount()
        );
        int failureCount = experimentRun.getCancelledCount()
                + experimentRun.getCompensatedCount()
                + experimentRun.getReconcileRequiredCount();

        return new ExperimentRunResponse(
                experimentRun.getRunId(),
                experimentRun.getTotalOrders(),
                metrics,
                failureCount,
                experimentRun.getCompensatedCount(),
                experimentRun.getCreatedAt()
        );
    }
}
