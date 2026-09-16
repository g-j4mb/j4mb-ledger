package com.j4mb.ledger.journal.api;

import com.j4mb.ledger.journal.domain.JournalHead;
import com.j4mb.ledger.journal.domain.JournalStatus;
import com.j4mb.ledger.journal.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalResponse(
        UUID id, String journalNumber, TransactionType transactionType, JournalStatus status,
        boolean reversed, UUID fiscalPeriodId, String currencyCode,
        String description, String reference,
        BigDecimal totalDebit, BigDecimal totalCredit,
        String postedBy, Instant postedAtUtc,
        List<JournalLineResponse> lines
) {
    public static JournalResponse from(JournalHead h, List<JournalLineResponse> lines) {
        return new JournalResponse(h.getId(), h.getJournalNumber(), h.getTransactionType(),
                h.getStatus(), h.isReversed(), h.getFiscalPeriodId(), h.getCurrencyCode(),
                h.getDescription(), h.getReference(), h.getTotalDebit(), h.getTotalCredit(),
                h.getPostedBy(), h.getPostedAtUtc(), lines);
    }
}
