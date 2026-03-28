package com.financetoy.reconciliation;

import com.financetoy.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "reconciliation_job")
public class ReconciliationJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String jobId;

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
            int matchedCount,
            int mismatchCount,
            int compensatedCount,
            int unresolvedCount,
            String unresolvedSummary
    ) {
        this.jobId = jobId;
        this.matchedCount = matchedCount;
        this.mismatchCount = mismatchCount;
        this.compensatedCount = compensatedCount;
        this.unresolvedCount = unresolvedCount;
        this.unresolvedSummary = unresolvedSummary;
    }

    public String getJobId() {
        return jobId;
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
