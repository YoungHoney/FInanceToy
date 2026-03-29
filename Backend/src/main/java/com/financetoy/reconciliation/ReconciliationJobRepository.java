package com.financetoy.reconciliation;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, Long> {

    Optional<ReconciliationJob> findByJobId(String jobId);

    Optional<ReconciliationJob> findByBusinessDate(LocalDate businessDate);
}
