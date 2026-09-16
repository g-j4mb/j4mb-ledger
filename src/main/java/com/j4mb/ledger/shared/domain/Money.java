package com.j4mb.ledger.shared.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currencyCode) {
    public Money {
        Objects.requireNonNull(amount, "amount required");
        Objects.requireNonNull(currencyCode, "currencyCode required");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Money amount cannot be negative");
    }
    public Money add(Money other) {
        if (!this.currencyCode.equals(other.currencyCode))
            throw new IllegalArgumentException("Cannot add different currencies: " + currencyCode + " vs " + other.currencyCode);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }
    public boolean isZero() { return amount.compareTo(BigDecimal.ZERO) == 0; }
}
