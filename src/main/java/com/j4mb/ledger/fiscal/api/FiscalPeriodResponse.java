package com.j4mb.ledger.fiscal.api;

import com.j4mb.ledger.fiscal.domain.FiscalPeriod;
import com.j4mb.ledger.fiscal.domain.PeriodStatus;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalPeriodResponse(
        UUID id, UUID fiscalYearId, String periodName, short periodNumber,
        LocalDate startDate, LocalDate endDate, PeriodStatus status
) {
    public static FiscalPeriodResponse from(FiscalPeriod p) {
        return new FiscalPeriodResponse(p.getId(), p.getFiscalYearId(), p.getPeriodName(),
                p.getPeriodNumber(), p.getStartDate(), p.getEndDate(), p.getStatus());
    }
}
