package com.j4mb.ledger.journal.api;

import com.j4mb.ledger.journal.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateJournalRequest(
        @Schema(description = "UUID of the open fiscal period this journal belongs to.", example = "550e8400-e29b-41d4-a716-446655440002") @NotNull UUID fiscalPeriodId,
        @Schema(description = "Business transaction type.", example = "SALE") @NotNull TransactionType transactionType,
        @Schema(description = "Header currency code (ISO 4217). Lines may use different currencies for FX entries.", example = "MYR") @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @Schema(description = "Human-readable journal description.", example = "Invoice INV-001 — Customer sale") String description,
        @Schema(description = "External reference number.", example = "INV-001") String reference,
        @Schema(description = "Originating system for idempotency tracking.", example = "sales-service") String sourceSystem,
        @Schema(description = "Unique document ID in the originating system.", example = "INV-001") String sourceDocumentId,
        @Schema(description = "Debit and credit lines. Must balance: SUM(DEBIT amounts) = SUM(CREDIT amounts).") @NotEmpty @Valid List<JournalLineRequest> lines
) {}
