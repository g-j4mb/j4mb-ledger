package com.j4mb.ledger.journal.api;

import com.j4mb.ledger.journal.domain.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record JournalLineRequest(
        @Schema(description = "UUID of the operational account to debit or credit.", example = "550e8400-e29b-41d4-a716-446655440003") @NotNull UUID accountId,
        @Schema(description = "DEBIT or CREDIT.", example = "DEBIT") @NotNull EntryType entryType,
        @Schema(description = "Positive line amount. Always > 0; direction is expressed by entryType.", example = "5000.00") @NotNull @DecimalMin("0.0001") BigDecimal amount,
        @Schema(description = "Transaction currency for this line (ISO 4217).", example = "MYR") @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @Schema(description = "Optional line-level description.", example = "Cash received from customer") String description
) {}
