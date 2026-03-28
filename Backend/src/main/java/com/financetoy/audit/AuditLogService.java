package com.financetoy.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String actionType, String domainType, String domainId, String detail) {
        auditLogRepository.save(new AuditLog(actionType, domainType, domainId, detail));
    }
}
