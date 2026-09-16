package com.j4mb.ledger.currency.api;

import com.j4mb.ledger.currency.domain.Currency;
import java.util.UUID;

public record CurrencyResponse(
        UUID id,
        String currencyCode,
        String currencyName,
        boolean baseCurrency,
        boolean active,
        int decimalPlaces
) {
    public static CurrencyResponse from(Currency c) {
        return new CurrencyResponse(c.getId(), c.getCurrencyCode(), c.getCurrencyName(),
                c.isBaseCurrency(), c.isActive(), c.getDecimalPlaces());
    }
}
