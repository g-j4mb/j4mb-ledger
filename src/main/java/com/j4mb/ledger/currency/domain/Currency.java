package com.j4mb.ledger.currency.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cur_currencies")
public class Currency {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "currency_code", nullable = false, unique = true, length = 3)
    private String currencyCode;

    @Column(name = "currency_name", nullable = false, length = 100)
    private String currencyName;

    @Column(name = "is_base_currency", nullable = false)
    private boolean baseCurrency;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "decimal_places", nullable = false)
    private int decimalPlaces;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    protected Currency() {}

    public static Currency create(String code, String name, boolean isBase, int decimalPlaces,
                                   String createdBy) {
        Currency c       = new Currency();
        c.id             = UUID.randomUUID();
        c.currencyCode   = code;
        c.currencyName   = name;
        c.baseCurrency   = isBase;
        c.active         = true;
        c.decimalPlaces  = decimalPlaces;
        c.createdBy      = createdBy;
        return c;
    }

    public void updateName(String name)         { if (name != null) this.currencyName = name; }
    public void setActive(boolean active)       { this.active = active; }
    public void updateDecimalPlaces(int places) { this.decimalPlaces = places; }

    public UUID    getId()             { return id; }
    public String  getCurrencyCode()   { return currencyCode; }
    public String  getCurrencyName()   { return currencyName; }
    public boolean isBaseCurrency()    { return baseCurrency; }
    public boolean isActive()          { return active; }
    public int     getDecimalPlaces()  { return decimalPlaces; }
    public String  getCreatedBy()      { return createdBy; }
}
