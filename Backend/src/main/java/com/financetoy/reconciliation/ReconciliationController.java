package com.financetoy.reconciliation;

import com.financetoy.reconciliation.dto.ReconciliationResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    public ResponseEntity<ReconciliationResultResponse> run() {
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliationService.run());
    }

    @GetMapping("/{jobId}")
    public ReconciliationResultResponse getJob(@PathVariable String jobId) {
        return reconciliationService.getJob(jobId);
    }
}
