package com.j4mb.ledger.currency.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cur_exchange_rates")
public class ExchangeRate {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 20)
    private RateType rateType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    protected ExchangeRate() {}

    public static ExchangeRate create(String from, String to, BigDecimal rate, RateType rateType,
                                       LocalDate effectiveDate, String createdBy) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Exchange rate must be positive");
        ExchangeRate r  = new ExchangeRate();
        r.id            = UUID.randomUUID();
        r.fromCurrency  = from;
        r.toCurrency    = to;
        r.rate          = rate;
        r.rateType      = rateType;
        r.effectiveDate = effectiveDate;
        r.createdBy     = createdBy;
        return r;
    }

    public void updateRate(BigDecimal rate)   { this.rate = rate; }
    public void updateSource(String source)  { this.source = source; }

    public UUID       getId()            { return id; }
    public String     getFromCurrency()  { return fromCurrency; }
    public String     getToCurrency()    { return toCurrency; }
    public BigDecimal getRate()          { return rate; }
    public RateType   getRateType()      { return rateType; }
    public LocalDate  getEffectiveDate() { return effectiveDate; }
    public String     getSource()        { return source; }
    public Instant    getCreatedAtUtc()  { return createdAtUtc; }
    public String     getCreatedBy()     { return createdBy; }
}
