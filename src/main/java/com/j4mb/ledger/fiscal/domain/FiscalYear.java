package com.j4mb.ledger.fiscal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fsc_fiscal_years")
public class FiscalYear {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "year_name", nullable = false, unique = true, length = 20)
    private String yearName;

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

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    protected FiscalYear() {}

    public static FiscalYear create(String yearName, LocalDate startDate, LocalDate endDate,
                                    String createdBy) {
        FiscalYear fy  = new FiscalYear();
        fy.id          = UUID.randomUUID();
        fy.yearName    = yearName;
        fy.startDate   = startDate;
        fy.endDate     = endDate;
        fy.status      = PeriodStatus.OPEN;
        fy.createdBy   = createdBy;
        fy.updatedBy   = createdBy;
        return fy;
    }

    public void close(String closedBy) {
        if (this.status != PeriodStatus.OPEN) throw new IllegalStateException("Fiscal year is not OPEN");
        this.status       = PeriodStatus.CLOSED;
        this.closedAtUtc  = Instant.now();
        this.closedBy     = closedBy;
    }

    public void lock(String lockedBy) {
        if (this.status != PeriodStatus.CLOSED) throw new IllegalStateException("Fiscal year must be CLOSED before locking");
        this.status      = PeriodStatus.LOCKED;
        this.closedAtUtc = Instant.now();
        this.closedBy    = lockedBy;
    }

    public void updateYearName(String yearName)    { if (yearName != null) this.yearName = yearName; }
    public void updateStartDate(LocalDate startDate) { if (startDate != null) this.startDate = startDate; }
    public void updateEndDate(LocalDate endDate)     { if (endDate != null) this.endDate = endDate; }

    public boolean isOpen()   { return status == PeriodStatus.OPEN; }
    public boolean isClosed() { return status == PeriodStatus.CLOSED; }
    public boolean isLocked() { return status == PeriodStatus.LOCKED; }

    public UUID         getId()            { return id; }
    public String       getYearName()      { return yearName; }
    public LocalDate    getStartDate()     { return startDate; }
    public LocalDate    getEndDate()       { return endDate; }
    public PeriodStatus getStatus()        { return status; }
    public Instant      getClosedAtUtc()   { return closedAtUtc; }
    public String       getClosedBy()      { return closedBy; }
    public Long         getVersion()       { return version; }
    public Instant      getCreatedAtUtc()  { return createdAtUtc; }
    public String       getCreatedBy()     { return createdBy; }
    public String       getUpdatedBy()     { return updatedBy; }
}
