package com.j4mb.ledger.fiscal.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateFiscalYearRequest(
        @Schema(description = "Unique fiscal year label.", example = "FY2026") @NotBlank @Size(max = 20) String yearName,
        @Schema(description = "First day of the fiscal year.", example = "2026-01-01") @NotNull LocalDate startDate,
        @Schema(description = "Last day of the fiscal year.", example = "2026-12-31") @NotNull LocalDate endDate
) {}
