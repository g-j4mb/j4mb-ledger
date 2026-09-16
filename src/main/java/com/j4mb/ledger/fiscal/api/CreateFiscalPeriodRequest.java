package com.j4mb.ledger.fiscal.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateFiscalPeriodRequest(
        @Schema(description = "Period label, e.g. month name or number.", example = "2026-01") @NotBlank @Size(max = 20) String periodName,
        @Schema(description = "Period sequence within the fiscal year (1–13).", example = "1") @Min(1) @Max(13) short periodNumber,
        @Schema(description = "First day of the period.", example = "2026-01-01") @NotNull LocalDate startDate,
        @Schema(description = "Last day of the period.", example = "2026-01-31") @NotNull LocalDate endDate
) {}
