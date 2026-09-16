package com.j4mb.ledger.fiscal.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateFiscalPeriodRequest(
        @Schema(description = "Updated period name.", example = "2026-01-A") @Size(max = 20) String periodName,
        @Schema(description = "Updated start date.") LocalDate startDate,
        @Schema(description = "Updated end date.") LocalDate endDate
) {}
