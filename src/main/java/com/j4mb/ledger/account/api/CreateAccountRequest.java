package com.j4mb.ledger.account.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAccountRequest(
        @Schema(description = "UUID of a postable COA leaf node.", example = "550e8400-e29b-41d4-a716-446655440001") @NotNull UUID coaNodeId,
        @Schema(description = "Unique account number within this tenant.", example = "ACC-1010-001") @NotBlank @Size(max = 30) String accountNumber,
        @Schema(description = "Descriptive account name.", example = "Main Cash Account") @NotBlank @Size(max = 200) String name,
        @Schema(description = "ISO 4217 currency code. One currency per account.", example = "MYR") @NotBlank @Size(min = 3, max = 3) String currencyCode
) {}
