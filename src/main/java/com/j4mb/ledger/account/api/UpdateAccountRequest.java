package com.j4mb.ledger.account.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateAccountRequest(
        @Schema(description = "Updated account name.") @Size(max = 200) String name,
        @Schema(description = "Updated description.") String description,
        @Schema(description = "Overdraft limit in account currency. 0 = no negative balance.") @DecimalMin("0.0") BigDecimal overdraftLimit,
        @Schema(description = "External reference number.") @Size(max = 100) String externalRef
) {}
