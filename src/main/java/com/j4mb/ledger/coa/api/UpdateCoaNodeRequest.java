package com.j4mb.ledger.coa.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateCoaNodeRequest(
        @Schema(description = "Updated display name.") @Size(max = 200) String name,
        @Schema(description = "Updated description.") String description,
        @Schema(description = "Display order among siblings.") Integer displayOrder,
        @Schema(description = "Whether overdraft is checked for accounts in this node.") Boolean allowNegativeBalance,
        @Schema(description = "Special accounting role: RETAINED_EARNINGS, INCOME_SUMMARY, or null.") String nodeRole,
        @Schema(description = "Whether this node accepts linked accounts.") Boolean postable
) {}
