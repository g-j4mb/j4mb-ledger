package com.j4mb.ledger.posting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdatePostingRuleRequest(
        @Schema(description = "Updated COA code for the debit leg.") @Size(max = 50) String debitCoaCode,
        @Schema(description = "Updated COA code for the credit leg.") @Size(max = 50) String creditCoaCode,
        @Schema(description = "Updated description template.") @Size(max = 500) String descriptionTemplate,
        @Schema(description = "Whether this rule is active.") Boolean active,
        @Schema(description = "Updated execution priority.") @Min(0) @Max(32767) Integer priority,
        @Schema(description = "Updated JSON conditions.") String conditions,
        @Schema(description = "Updated JSON multi-leg rule definitions.") String multiLegRules
) {}
