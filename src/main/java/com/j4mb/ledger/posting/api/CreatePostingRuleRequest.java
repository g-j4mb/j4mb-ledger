package com.j4mb.ledger.posting.api;

import com.j4mb.ledger.journal.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostingRuleRequest(
        @Schema(description = "Business event type this rule handles.", example = "SALES_INVOICE_POSTED")
        @NotBlank @Size(max = 100) String eventType,

        @Schema(description = "Human-readable rule name.", example = "Sales revenue recognition")
        @NotBlank @Size(max = 100) String ruleName,

        @Schema(description = "Journal transaction type to use when posting.", example = "SALE")
        @NotNull TransactionType transactionType,

        @Schema(description = "COA code for the debit leg.", example = "1-01-001")
        @NotBlank @Size(max = 50) String debitCoaCode,

        @Schema(description = "COA code for the credit leg.", example = "4-01-001")
        @NotBlank @Size(max = 50) String creditCoaCode,

        @Schema(description = "Description template with optional placeholders.", example = "Sales invoice {invoiceNumber}")
        @Size(max = 500) String descriptionTemplate,

        @Schema(description = "Execution priority. Higher = evaluated first.", example = "10")
        @Min(0) @Max(32767) int priority,

        @Schema(description = "Optional JSON conditions for conditional rule application.")
        String conditions,

        @Schema(description = "Optional JSON multi-leg rule definitions.")
        String multiLegRules
) {}
