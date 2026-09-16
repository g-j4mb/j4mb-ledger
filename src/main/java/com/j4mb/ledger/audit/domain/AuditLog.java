package com.j4mb.ledger.audit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aud_audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "performed_at_utc", nullable = false, updatable = false)
    private Instant performedAtUtc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "jsonb")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "jsonb")
    private String afterState;

    protected AuditLog() {}

    public static AuditLog create(String entityType, UUID entityId, String action,
                                   String performedBy, String beforeState, String afterState) {
        AuditLog log       = new AuditLog();
        log.id             = UUID.randomUUID();
        log.entityType     = entityType;
        log.entityId       = entityId;
        log.action         = action;
        log.performedBy    = performedBy;
        log.performedAtUtc = Instant.now();
        log.beforeState    = beforeState;
        log.afterState     = afterState;
        return log;
    }

    public UUID    getId()             { return id; }
    public String  getEntityType()     { return entityType; }
    public UUID    getEntityId()       { return entityId; }
    public String  getAction()         { return action; }
    public String  getPerformedBy()    { return performedBy; }
    public Instant getPerformedAtUtc() { return performedAtUtc; }
    public String  getBeforeState()    { return beforeState; }
    public String  getAfterState()     { return afterState; }
}
