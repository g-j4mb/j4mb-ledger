package com.j4mb.ledger.balance.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bal_account_balances")
public class AccountBalance {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "fiscal_period_id", nullable = false)
    private UUID fiscalPeriodId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "opening_debit",  nullable = false, precision = 20, scale = 4)
    private BigDecimal openingDebit  = BigDecimal.ZERO;

    @Column(name = "opening_credit", nullable = false, precision = 20, scale = 4)
    private BigDecimal openingCredit = BigDecimal.ZERO;

    @Column(name = "period_debit",   nullable = false, precision = 20, scale = 4)
    private BigDecimal periodDebit   = BigDecimal.ZERO;

    @Column(name = "period_credit",  nullable = false, precision = 20, scale = 4)
    private BigDecimal periodCredit  = BigDecimal.ZERO;

    @Column(name = "closing_debit",  nullable = false, precision = 20, scale = 4)
    private BigDecimal closingDebit  = BigDecimal.ZERO;

    @Column(name = "closing_credit", nullable = false, precision = 20, scale = 4)
    private BigDecimal closingCredit = BigDecimal.ZERO;

    @Column(name = "last_rebuilt_utc", nullable = false)
    private Instant lastRebuiltUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountBalance() {}

    public static AccountBalance open(UUID accountId, UUID fiscalPeriodId, String currencyCode,
                                       BigDecimal openingDebit, BigDecimal openingCredit,
                                       String createdBy) {
        AccountBalance b   = new AccountBalance();
        b.id               = UUID.randomUUID();
        b.accountId        = accountId;
        b.fiscalPeriodId   = fiscalPeriodId;
        b.currencyCode     = currencyCode;
        b.openingDebit     = openingDebit;
        b.openingCredit    = openingCredit;
        b.closingDebit     = openingDebit;
        b.closingCredit    = openingCredit;
        b.createdBy        = createdBy;
        b.updatedBy        = createdBy;
        return b;
    }

    public void applyDebit(BigDecimal amount) {
        this.periodDebit   = this.periodDebit.add(amount);
        this.closingDebit  = this.openingDebit.add(this.periodDebit);
        this.lastRebuiltUtc = Instant.now();
    }

    public void applyCredit(BigDecimal amount) {
        this.periodCredit  = this.periodCredit.add(amount);
        this.closingCredit = this.openingCredit.add(this.periodCredit);
        this.lastRebuiltUtc = Instant.now();
    }

    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public UUID       getId()             { return id; }
    public UUID       getAccountId()      { return accountId; }
    public UUID       getFiscalPeriodId() { return fiscalPeriodId; }
    public String     getCurrencyCode()   { return currencyCode; }
    public BigDecimal getOpeningDebit()   { return openingDebit; }
    public BigDecimal getOpeningCredit()  { return openingCredit; }
    public BigDecimal getPeriodDebit()    { return periodDebit; }
    public BigDecimal getPeriodCredit()   { return periodCredit; }
    public BigDecimal getClosingDebit()   { return closingDebit; }
    public BigDecimal getClosingCredit()  { return closingCredit; }
    public String     getCreatedBy()      { return createdBy; }
    public String     getUpdatedBy()      { return updatedBy; }
    public Long       getVersion()        { return version; }
}
