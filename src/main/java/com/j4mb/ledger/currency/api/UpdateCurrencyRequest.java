package com.j4mb.ledger.currency.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCurrencyRequest(
        @Schema(description = "Updated currency name.") @Size(max = 100) String currencyName,
        @Schema(description = "Whether this currency is active.") Boolean active,
        @Schema(description = "Decimal places.") @Min(0) @Max(8) Integer decimalPlaces
) {}
