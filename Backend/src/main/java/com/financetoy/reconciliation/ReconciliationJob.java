package com.financetoy.reconciliation;

import com.financetoy.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_job")
public class ReconciliationJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String jobId;

    @Column(nullable = false, unique = true)
    private LocalDate businessDate;

    @Column(nullable = false)
    private int matchedCount;

    @Column(nullable = false)
    private int mismatchCount;

    @Column(nullable = false)
    private int compensatedCount;

    @Column(nullable = false)
    private int unresolvedCount;

    @Lob
    @Column(nullable = false)
    private String unresolvedSummary;

    protected ReconciliationJob() {
    }

    public ReconciliationJob(
            String jobId,
            LocalDate businessDate,
            int matchedCount,
            int mismatchCount,
            int compensatedCount,
            int unresolvedCount,
            String unresolvedSummary
    ) {
        this.jobId = jobId;
        this.businessDate = businessDate;
        this.matchedCount = matchedCount;
        this.mismatchCount = mismatchCount;
        this.compensatedCount = compensatedCount;
        this.unresolvedCount = unresolvedCount;
        this.unresolvedSummary = unresolvedSummary;
    }

    public void overwriteResult(
            int matchedCount,
            int mismatchCount,
            int compensatedCount,
            int unresolvedCount,
            String unresolvedSummary
    ) {
        this.matchedCount = matchedCount;
        this.mismatchCount = mismatchCount;
        this.compensatedCount = compensatedCount;
        this.unresolvedCount = unresolvedCount;
        this.unresolvedSummary = unresolvedSummary;
    }

    public String getJobId() {
        return jobId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public int getMismatchCount() {
        return mismatchCount;
    }

    public int getCompensatedCount() {
        return compensatedCount;
    }

    public int getUnresolvedCount() {
        return unresolvedCount;
    }

    public String getUnresolvedSummary() {
        return unresolvedSummary;
    }
}
