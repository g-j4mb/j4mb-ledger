package com.j4mb.ledger.journal.api;

import com.j4mb.ledger.journal.domain.EntryType;
import com.j4mb.ledger.journal.domain.JournalLine;
import java.math.BigDecimal;
import java.util.UUID;

public record JournalLineResponse(
        UUID id, UUID accountId, EntryType entryType,
        BigDecimal amount, String currencyCode, String description
) {
    public static JournalLineResponse from(JournalLine l) {
        return new JournalLineResponse(l.getId(), l.getAccountId(), l.getEntryType(),
                l.getAmount(), l.getCurrencyCode(), l.getDescription());
    }
}
