package com.j4mb.ledger.coa.api;

import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.NormalBalance;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCoaNodeRequest(
        @Schema(description = "UUID of the parent COA node. Null for root nodes.", example = "550e8400-e29b-41d4-a716-446655440000", nullable = true) UUID parentId,
        @Schema(description = "Unique COA code within this tenant.", example = "1010") @NotBlank @Size(max = 50) String code,
        @Schema(description = "Descriptive name for this account node.", example = "Cash") @NotBlank @Size(max = 200) String name,
        @Schema(description = "Account classification.", example = "ASSET") @NotNull AccountType accountType,
        @Schema(description = "Normal balance side for this account type.", example = "DEBIT") @NotNull NormalBalance normalBalance,
        @Schema(description = "True if this leaf node can have operational accounts linked to it.", example = "true") boolean postable,
        @Schema(description = "Display order among siblings.", example = "1") int displayOrder,
        @Schema(description = "If false, overdraft limits are enforced for accounts under this node at posting time. Default true.", example = "false") Boolean allowNegativeBalance
) {}
