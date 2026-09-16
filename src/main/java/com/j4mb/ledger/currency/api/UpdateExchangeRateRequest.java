package com.j4mb.ledger.currency.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateExchangeRateRequest(
        @Schema(description = "Updated rate. Must be > 0.", example = "4.75") @DecimalMin("0.0001") BigDecimal rate,
        @Schema(description = "Source of the rate.", example = "BNM") String source
) {}
