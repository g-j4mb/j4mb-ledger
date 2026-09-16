package com.j4mb.ledger.journal.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateJournalRequest(
        @Schema(description = "Updated description.") String description,
        @Schema(description = "Updated external reference.") String reference
) {}
