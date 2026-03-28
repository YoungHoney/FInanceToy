package com.financetoy.experiment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {

    Optional<ExperimentRun> findByRunId(String runId);
}
