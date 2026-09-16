package com.j4mb.ledger.journal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jnl_journal_lines")
public class JournalLine {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "line_number", nullable = false)
    private short lineNumber;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "base_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal baseAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "description")
    private String description;

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    @Column(name = "project_code", length = 50)
    private String projectCode;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    protected JournalLine() {}

    public static JournalLine create(UUID journalId, short lineNumber, UUID accountId,
                                     EntryType entryType, BigDecimal amount,
                                     String currencyCode, String baseCurrencyCode,
                                     BigDecimal baseAmount, BigDecimal exchangeRate,
                                     String createdBy) {
        JournalLine l      = new JournalLine();
        l.id               = UUID.randomUUID();
        l.journalId        = journalId;
        l.lineNumber       = lineNumber;
        l.accountId        = accountId;
        l.entryType        = entryType;
        l.amount           = amount;
        l.currencyCode     = currencyCode;
        l.baseCurrencyCode = baseCurrencyCode;
        l.baseAmount       = baseAmount;
        l.exchangeRate     = exchangeRate;
        l.createdBy        = createdBy;
        return l;
    }

    public boolean isDebit()  { return entryType == EntryType.DEBIT; }
    public boolean isCredit() { return entryType == EntryType.CREDIT; }

    public UUID       getId()               { return id; }
    public UUID       getJournalId()        { return journalId; }
    public short      getLineNumber()       { return lineNumber; }
    public UUID       getAccountId()        { return accountId; }
    public EntryType  getEntryType()        { return entryType; }
    public BigDecimal getAmount()           { return amount; }
    public String     getCurrencyCode()     { return currencyCode; }
    public String     getBaseCurrencyCode() { return baseCurrencyCode; }
    public BigDecimal getBaseAmount()       { return baseAmount; }
    public BigDecimal getExchangeRate()     { return exchangeRate; }
    public String     getDescription()      { return description; }
    public String     getCreatedBy()        { return createdBy; }
}
