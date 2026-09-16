package com.j4mb.ledger.journal.service;

import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.shared.exception.UnbalancedJournalException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DoubleEntryValidator {

    public void validate(List<JournalLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new UnbalancedJournalException(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal totalDebits = lines.stream()
                .filter(JournalLine::isDebit)
                .map(JournalLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = lines.stream()
                .filter(JournalLine::isCredit)
                .map(JournalLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new UnbalancedJournalException(totalDebits, totalCredits);
        }
    }
}
