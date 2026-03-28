package com.financetoy.audit;

import com.financetoy.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String actionType;

    @Column(nullable = false, length = 100)
    private String domainType;

    @Column(nullable = false, length = 100)
    private String domainId;

    @Column(nullable = false, length = 1000)
    private String detail;

    protected AuditLog() {
    }

    public AuditLog(String actionType, String domainType, String domainId, String detail) {
        this.actionType = actionType;
        this.domainType = domainType;
        this.domainId = domainId;
        this.detail = detail;
    }

    public String getActionType() {
        return actionType;
    }

    public String getDomainType() {
        return domainType;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDetail() {
        return detail;
    }
}
