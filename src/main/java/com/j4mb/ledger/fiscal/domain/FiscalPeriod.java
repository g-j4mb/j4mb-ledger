package com.j4mb.ledger.fiscal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fsc_periods")
public class FiscalPeriod {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fiscal_year_id", nullable = false)
    private UUID fiscalYearId;

    @Column(name = "period_name", nullable = false, length = 20)
    private String periodName;

    @Column(name = "period_number", nullable = false)
    private short periodNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PeriodStatus status;

    @Column(name = "closed_at_utc")
    private Instant closedAtUtc;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    protected FiscalPeriod() {}

    public static FiscalPeriod create(UUID fiscalYearId, String periodName, short periodNumber,
                                      LocalDate startDate, LocalDate endDate, String createdBy) {
        FiscalPeriod p  = new FiscalPeriod();
        p.id            = UUID.randomUUID();
        p.fiscalYearId  = fiscalYearId;
        p.periodName    = periodName;
        p.periodNumber  = periodNumber;
        p.startDate     = startDate;
        p.endDate       = endDate;
        p.status        = PeriodStatus.OPEN;
        p.createdBy     = createdBy;
        return p;
    }

    public void close(String closedBy) {
        if (this.status != PeriodStatus.OPEN) throw new IllegalStateException("Period is not OPEN");
        this.status       = PeriodStatus.CLOSED;
        this.closedAtUtc  = Instant.now();
        this.closedBy     = closedBy;
    }

    public void reopen(String reopenedBy) {
        if (this.status != PeriodStatus.CLOSED)
            throw new IllegalStateException("Only CLOSED periods can be reopened");
        this.status      = PeriodStatus.OPEN;
        this.closedAtUtc = null;
        this.closedBy    = null;
    }

    public void lock(String lockedBy) {
        if (this.status != PeriodStatus.CLOSED) throw new IllegalStateException("Period must be CLOSED before locking");
        this.status      = PeriodStatus.LOCKED;
        this.closedAtUtc = Instant.now();
        this.closedBy    = lockedBy;
    }

    public void updatePeriodName(String periodName)  { if (periodName != null) this.periodName = periodName; }
    public void updateStartDate(LocalDate startDate) { if (startDate != null) this.startDate = startDate; }
    public void updateEndDate(LocalDate endDate)     { if (endDate != null) this.endDate = endDate; }

    public boolean isOpen()   { return status == PeriodStatus.OPEN; }
    public boolean isClosed() { return status == PeriodStatus.CLOSED; }
    public boolean isLocked() { return status == PeriodStatus.LOCKED; }

    public UUID         getId()           { return id; }
    public UUID         getFiscalYearId() { return fiscalYearId; }
    public String       getPeriodName()   { return periodName; }
    public short        getPeriodNumber() { return periodNumber; }
    public LocalDate    getStartDate()    { return startDate; }
    public LocalDate    getEndDate()      { return endDate; }
    public PeriodStatus getStatus()       { return status; }
    public Instant      getClosedAtUtc()  { return closedAtUtc; }
    public Long         getVersion()      { return version; }
    public String       getCreatedBy()    { return createdBy; }
}
