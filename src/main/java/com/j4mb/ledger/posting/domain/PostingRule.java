package com.j4mb.ledger.posting.domain;

import com.j4mb.ledger.journal.domain.TransactionType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pst_posting_rules")
public class PostingRule {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "debit_coa_code", nullable = false, length = 50)
    private String debitCoaCode;

    @Column(name = "credit_coa_code", nullable = false, length = 50)
    private String creditCoaCode;

    @Column(name = "description_template", length = 500)
    private String descriptionTemplate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "priority", nullable = false)
    private short priority;

    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @Column(name = "multi_leg_rules", columnDefinition = "jsonb")
    private String multiLegRules;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    protected PostingRule() {}

    public static PostingRule create(String eventType, String ruleName, TransactionType transactionType,
                                     String debitCoaCode, String creditCoaCode,
                                     String descriptionTemplate, short priority, String createdBy) {
        PostingRule r         = new PostingRule();
        r.id                  = java.util.UUID.randomUUID();
        r.eventType           = eventType;
        r.ruleName            = ruleName;
        r.transactionType     = transactionType;
        r.debitCoaCode        = debitCoaCode;
        r.creditCoaCode       = creditCoaCode;
        r.descriptionTemplate = descriptionTemplate;
        r.active              = true;
        r.priority            = priority;
        r.createdBy           = createdBy;
        r.updatedBy           = createdBy;
        r.createdAtUtc        = Instant.now();
        r.updatedAtUtc        = Instant.now();
        return r;
    }

    public void updateDebitCoaCode(String code)               { this.debitCoaCode = code; this.updatedAtUtc = Instant.now(); }
    public void updateCreditCoaCode(String code)              { this.creditCoaCode = code; this.updatedAtUtc = Instant.now(); }
    public void updateDescriptionTemplate(String template)    { this.descriptionTemplate = template; this.updatedAtUtc = Instant.now(); }
    public void updateActive(boolean active)                  { this.active = active; this.updatedAtUtc = Instant.now(); }
    public void updatePriority(short priority)                { this.priority = priority; this.updatedAtUtc = Instant.now(); }
    public void updateConditions(String conditions)           { this.conditions = conditions; this.updatedAtUtc = Instant.now(); }
    public void updateMultiLegRules(String multiLegRules)     { this.multiLegRules = multiLegRules; this.updatedAtUtc = Instant.now(); }
    public void updateUpdatedBy(String updatedBy)             { this.updatedBy = updatedBy; }

    public UUID            getId()                  { return id; }
    public String          getEventType()           { return eventType; }
    public String          getRuleName()            { return ruleName; }
    public TransactionType getTransactionType()     { return transactionType; }
    public String          getDebitCoaCode()        { return debitCoaCode; }
    public String          getCreditCoaCode()       { return creditCoaCode; }
    public String          getDescriptionTemplate() { return descriptionTemplate; }
    public boolean         isActive()               { return active; }
    public short           getPriority()            { return priority; }
    public String          getConditions()          { return conditions; }
    public String          getMultiLegRules()       { return multiLegRules; }
    public Instant         getCreatedAtUtc()        { return createdAtUtc; }
    public Instant         getUpdatedAtUtc()        { return updatedAtUtc; }
    public String          getCreatedBy()           { return createdBy; }
    public String          getUpdatedBy()           { return updatedBy; }
}
