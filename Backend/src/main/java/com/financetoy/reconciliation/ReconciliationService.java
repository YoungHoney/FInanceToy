package com.financetoy.reconciliation;

import com.financetoy.audit.AuditLogService;
import com.financetoy.common.exception.NotFoundException;
import com.financetoy.ledger.LedgerEntryRepository;
import com.financetoy.ledger.LedgerEntryType;
import com.financetoy.order.OrderEventRepository;
import com.financetoy.order.OrderEventType;
import com.financetoy.order.OrderStatus;
import com.financetoy.order.TradeOrder;
import com.financetoy.order.TradeOrderRepository;
import com.financetoy.reconciliation.dto.ReconciliationResultResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

    private final TradeOrderRepository tradeOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationJobRepository reconciliationJobRepository;
    private final AuditLogService auditLogService;

    public ReconciliationService(
            TradeOrderRepository tradeOrderRepository,
            OrderEventRepository orderEventRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ReconciliationJobRepository reconciliationJobRepository,
            AuditLogService auditLogService
    ) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.reconciliationJobRepository = reconciliationJobRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReconciliationResultResponse run() {
        LocalDate businessDate = LocalDate.now();
        List<String> unresolvedItems = new ArrayList<>();
        int matchedCount = 0;
        int mismatchCount = 0;
        int compensatedCount = 0;
        int unresolvedCount = 0;

        for (TradeOrder order : tradeOrderRepository.findAll()) {
            long reserveCount = ledgerEntryRepository.countByOrderIdAndEntryType(order.getOrderId(), LedgerEntryType.RESERVE_CASH);
            long executeCount = ledgerEntryRepository.countByOrderIdAndEntryType(order.getOrderId(), LedgerEntryType.EXECUTE_BUY);
            long releaseCount = ledgerEntryRepository.countByOrderIdAndEntryType(order.getOrderId(), LedgerEntryType.RELEASE_CASH);
            long compensationCount = ledgerEntryRepository.countByOrderIdAndEntryType(order.getOrderId(), LedgerEntryType.APPLY_COMPENSATION);
            boolean hasDelayedCallback = orderEventRepository.existsByOrderIdAndEventType(order.getOrderId(), OrderEventType.DELAYED_CALLBACK_RECEIVED);

            boolean matched = switch (order.getStatus()) {
                case EXECUTED -> reserveCount == 1 && executeCount == 1 && releaseCount == 0 && compensationCount == 0;
                case CANCELLED -> reserveCount == 1 && executeCount == 0 && releaseCount == 1 && compensationCount == 0;
                case COMPENSATED -> reserveCount == 1 && executeCount == 0 && releaseCount == 0 && compensationCount == 1;
                default -> false;
            };

            if (matched && !hasDelayedCallback) {
                matchedCount++;
                if (order.getStatus() == OrderStatus.COMPENSATED) {
                    compensatedCount++;
                }
                continue;
            }

            mismatchCount++;
            unresolvedCount++;
            unresolvedItems.add(order.getOrderId() + " - status=" + order.getStatus() + ", executionResult=" + order.getExecutionResult());
        }

        String unresolvedSummary = unresolvedItems.isEmpty() ? "NONE" : String.join("\n", unresolvedItems);
        final int finalMatchedCount = matchedCount;
        final int finalMismatchCount = mismatchCount;
        final int finalCompensatedCount = compensatedCount;
        final int finalUnresolvedCount = unresolvedCount;
        final String finalUnresolvedSummary = unresolvedSummary;
        ReconciliationJob job = reconciliationJobRepository.findByBusinessDate(businessDate)
                .map(existingJob -> {
                    existingJob.overwriteResult(
                            finalMatchedCount,
                            finalMismatchCount,
                            finalCompensatedCount,
                            finalUnresolvedCount,
                            finalUnresolvedSummary
                    );
                    return existingJob;
                })
                .orElseGet(() -> new ReconciliationJob(
                        UUID.randomUUID().toString(),
                        businessDate,
                        finalMatchedCount,
                        finalMismatchCount,
                        finalCompensatedCount,
                        finalUnresolvedCount,
                        finalUnresolvedSummary
                ));

        job = reconciliationJobRepository.save(job);
        auditLogService.record("RECONCILIATION_RUN", "RECONCILIATION", job.getJobId(), "배치 대사 실행 완료");
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public ReconciliationResultResponse getJob(String jobId) {
        ReconciliationJob job = reconciliationJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new NotFoundException("대사 작업을 찾을 수 없습니다. jobId=" + jobId));
        return toResponse(job);
    }

    private ReconciliationResultResponse toResponse(ReconciliationJob job) {
        List<String> unresolvedItems = "NONE".equals(job.getUnresolvedSummary())
                ? List.of()
                : Arrays.asList(job.getUnresolvedSummary().split("\n"));

        return new ReconciliationResultResponse(
                job.getJobId(),
                job.getBusinessDate(),
                job.getMatchedCount(),
                job.getMismatchCount(),
                job.getCompensatedCount(),
                job.getUnresolvedCount(),
                unresolvedItems,
                job.getUpdatedAt(),
                job.getCreatedAt()
        );
    }
}
