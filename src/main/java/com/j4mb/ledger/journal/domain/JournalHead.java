package com.j4mb.ledger.journal.domain;

import com.j4mb.ledger.shared.exception.LedgerException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jnl_journal_heads")
public class JournalHead {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "journal_number", nullable = false, unique = true, length = 30)
    private String journalNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JournalStatus status;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Column(name = "fiscal_period_id", nullable = false)
    private UUID fiscalPeriodId;

    @Column(name = "description")
    private String description;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "source_system", length = 50)
    private String sourceSystem;

    @Column(name = "source_document_id", length = 100)
    private String sourceDocumentId;

    @Column(name = "reversal_of_journal_id")
    private UUID reversalOfJournalId;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @Column(name = "posted_by", length = 255)
    private String postedBy;

    @Column(name = "posted_at_utc")
    private Instant postedAtUtc;

    @Column(name = "total_debit", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    protected JournalHead() {}

    public static JournalHead create(String journalNumber, TransactionType type,
                                     UUID fiscalPeriodId, String currencyCode, String description,
                                     String createdBy) {
        JournalHead h       = new JournalHead();
        h.id                = UUID.randomUUID();
        h.journalNumber     = journalNumber;
        h.transactionType   = type;
        h.status            = JournalStatus.DRAFT;
        h.reversed          = false;
        h.fiscalPeriodId    = fiscalPeriodId;
        h.currencyCode      = currencyCode;
        h.description       = description;
        h.createdBy         = createdBy;
        h.updatedBy         = createdBy;
        return h;
    }

    public void markPosted(String postedBy) {
        this.status      = JournalStatus.POSTED;
        this.postedBy    = postedBy;
        this.postedAtUtc = Instant.now();
        this.updatedAtUtc = Instant.now();
    }

    public void cancel() {
        if (this.status != JournalStatus.DRAFT)
            throw new LedgerException("Only DRAFT journals can be cancelled");
        this.status = JournalStatus.CANCELLED;
        this.updatedAtUtc = Instant.now();
    }

    public void markReversed() { this.reversed = true; }

    public void updateDescription(String description) {
        this.description  = description;
        this.updatedAtUtc = Instant.now();
    }

    public void updateReference(String reference) {
        this.reference    = reference;
        this.updatedAtUtc = Instant.now();
    }

    public void updateTotals(BigDecimal totalDebit, BigDecimal totalCredit) {
        this.totalDebit  = totalDebit;
        this.totalCredit = totalCredit;
    }

    public boolean isDraft()      { return status == JournalStatus.DRAFT; }
    public boolean isPosted()     { return status == JournalStatus.POSTED; }
    public boolean isCancelled()  { return status == JournalStatus.CANCELLED; }
    public boolean isReversed()   { return reversed; }

    public UUID            getId()               { return id; }
    public String          getJournalNumber()    { return journalNumber; }
    public TransactionType getTransactionType()  { return transactionType; }
    public JournalStatus   getStatus()           { return status; }
    public UUID            getFiscalPeriodId()   { return fiscalPeriodId; }
    public String          getDescription()      { return description; }
    public String          getReference()        { return reference; }
    public String          getSourceSystem()     { return sourceSystem; }
    public String          getSourceDocumentId() { return sourceDocumentId; }
    public UUID            getReversalOfJournalId() { return reversalOfJournalId; }
    public String          getPostedBy()         { return postedBy; }
    public Instant         getPostedAtUtc()      { return postedAtUtc; }
    public BigDecimal      getTotalDebit()        { return totalDebit; }
    public BigDecimal      getTotalCredit()       { return totalCredit; }
    public String          getCurrencyCode()      { return currencyCode; }
    public Long            getVersion()           { return version; }
    public String          getCreatedBy()         { return createdBy; }
    public String          getUpdatedBy()         { return updatedBy; }

    void setReversalOf(UUID originalJournalId, String reason) {
        this.reversalOfJournalId = originalJournalId;
        this.reversalReason      = reason;
    }
}
