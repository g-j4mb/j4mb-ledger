package com.j4mb.ledger.posting.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pst_idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "source_event_id", nullable = false, length = 200)
    private String sourceEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "journal_id")
    private UUID journalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "processed_at_utc", nullable = false, updatable = false)
    private Instant processedAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    protected IdempotencyKey() {}

    public static IdempotencyKey processed(String sourceSystem, String sourceEventId,
                                            String eventType, UUID journalId, String createdBy) {
        IdempotencyKey k  = new IdempotencyKey();
        k.id              = UUID.randomUUID();
        k.sourceSystem    = sourceSystem;
        k.sourceEventId   = sourceEventId;
        k.eventType       = eventType;
        k.journalId       = journalId;
        k.status          = IdempotencyStatus.PROCESSED;
        k.createdBy       = createdBy;
        return k;
    }

    public UUID              getId()            { return id; }
    public String            getSourceSystem()  { return sourceSystem; }
    public String            getSourceEventId() { return sourceEventId; }
    public UUID              getJournalId()     { return journalId; }
    public IdempotencyStatus getStatus()        { return status; }
    public String            getCreatedBy()     { return createdBy; }
}
