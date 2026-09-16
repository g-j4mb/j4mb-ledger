package com.j4mb.ledger.currency.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCurrencyRequest(
        @Schema(description = "ISO 4217 3-letter currency code.", example = "MYR") @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @Schema(description = "Full currency name.", example = "Malaysian Ringgit") @NotBlank @Size(max = 100) String currencyName,
        @Schema(description = "Set to true to designate this as the tenant base currency. Only one allowed.", example = "true") boolean baseCurrency,
        @Schema(description = "Number of decimal places for this currency.", example = "2", minimum = "0", maximum = "8") @Min(0) @Max(8) int decimalPlaces
) {}
