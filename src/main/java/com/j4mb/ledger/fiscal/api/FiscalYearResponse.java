package com.j4mb.ledger.fiscal.api;

import com.j4mb.ledger.fiscal.domain.FiscalYear;
import com.j4mb.ledger.fiscal.domain.PeriodStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalYearResponse(
        UUID id, String yearName, LocalDate startDate, LocalDate endDate,
        PeriodStatus status, Instant createdAtUtc
) {
    public static FiscalYearResponse from(FiscalYear fy) {
        return new FiscalYearResponse(fy.getId(), fy.getYearName(), fy.getStartDate(),
                fy.getEndDate(), fy.getStatus(), fy.getCreatedAtUtc());
    }
}
