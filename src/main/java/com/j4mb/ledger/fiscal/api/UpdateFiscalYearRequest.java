package com.j4mb.ledger.fiscal.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateFiscalYearRequest(
        @Schema(description = "Updated year name.", example = "FY2026-R") @Size(max = 20) String yearName,
        @Schema(description = "Updated start date.") LocalDate startDate,
        @Schema(description = "Updated end date.") LocalDate endDate
) {}
