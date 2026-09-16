package com.j4mb.ledger.journal.service;

import com.j4mb.ledger.journal.domain.EntryType;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.shared.exception.UnbalancedJournalException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoubleEntryValidatorTest {

    private final DoubleEntryValidator validator = new DoubleEntryValidator();

    @Test
    void shouldPass_whenDebitsEqualCredits() {
        // Given
        List<JournalLine> lines = List.of(
                debitLine(new BigDecimal("1000.00")),
                creditLine(new BigDecimal("1000.00"))
        );

        // When / Then
        assertThatCode(() -> validator.validate(lines)).doesNotThrowAnyException();
    }

    @Test
    void shouldPass_whenMultipleDebitsAndCreditsBalance() {
        // Given — multiple debit and credit lines
        List<JournalLine> lines = List.of(
                debitLine(new BigDecimal("600.00")),
                debitLine(new BigDecimal("400.00")),
                creditLine(new BigDecimal("750.00")),
                creditLine(new BigDecimal("250.00"))
        );

        // When / Then
        assertThatCode(() -> validator.validate(lines)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrow_whenDebitsExceedCredits() {
        // Given
        List<JournalLine> lines = List.of(
                debitLine(new BigDecimal("1500.00")),
                creditLine(new BigDecimal("1000.00"))
        );

        // When / Then
        assertThatThrownBy(() -> validator.validate(lines))
                .isInstanceOf(UnbalancedJournalException.class)
                .hasMessageContaining("1500")
                .hasMessageContaining("1000");
    }

    @Test
    void shouldThrow_whenCreditsExceedDebits() {
        // Given
        List<JournalLine> lines = List.of(
                debitLine(new BigDecimal("500.00")),
                creditLine(new BigDecimal("800.00"))
        );

        // When / Then
        assertThatThrownBy(() -> validator.validate(lines))
                .isInstanceOf(UnbalancedJournalException.class);
    }

    @Test
    void shouldThrow_whenLinesAreEmpty() {
        // Given
        List<JournalLine> lines = List.of();

        // When / Then
        assertThatThrownBy(() -> validator.validate(lines))
                .isInstanceOf(UnbalancedJournalException.class);
    }

    @Test
    void shouldThrow_whenLinesAreNull() {
        // When / Then
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(UnbalancedJournalException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static JournalLine debitLine(BigDecimal amount) {
        return JournalLine.create(UUID.randomUUID(), (short) 1, UUID.randomUUID(),
                EntryType.DEBIT, amount, "MYR", "MYR", amount, BigDecimal.ONE, "test");
    }

    private static JournalLine creditLine(BigDecimal amount) {
        return JournalLine.create(UUID.randomUUID(), (short) 1, UUID.randomUUID(),
                EntryType.CREDIT, amount, "MYR", "MYR", amount, BigDecimal.ONE, "test");
    }
}
