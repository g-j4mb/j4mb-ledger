package com.j4mb.ledger.currency.api;

import com.j4mb.ledger.currency.domain.ExchangeRate;
import com.j4mb.ledger.currency.domain.RateType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        RateType rateType,
        LocalDate effectiveDate,
        String source,
        Instant createdAtUtc
) {
    public static ExchangeRateResponse from(ExchangeRate r) {
        return new ExchangeRateResponse(
                r.getId(), r.getFromCurrency(), r.getToCurrency(),
                r.getRate(), r.getRateType(), r.getEffectiveDate(),
                r.getSource(), r.getCreatedAtUtc());
    }
}
