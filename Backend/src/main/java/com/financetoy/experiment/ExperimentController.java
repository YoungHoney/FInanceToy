package com.financetoy.experiment;

import com.financetoy.experiment.dto.ExperimentRunRequest;
import com.financetoy.experiment.dto.ExperimentRunResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/run")
    public ResponseEntity<ExperimentRunResponse> runExperiment(@Valid @RequestBody ExperimentRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(experimentService.runExperiment(request));
    }

    @GetMapping("/{runId}")
    public ExperimentRunResponse getExperiment(@PathVariable String runId) {
        return experimentService.getExperiment(runId);
    }
}
