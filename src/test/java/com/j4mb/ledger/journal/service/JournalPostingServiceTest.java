package com.j4mb.ledger.journal.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.audit.service.AuditService;
import com.j4mb.ledger.balance.service.BalanceProjectionService;
import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.domain.NormalBalance;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.fiscal.service.FiscalPeriodService;
import com.j4mb.ledger.journal.domain.EntryType;
import com.j4mb.ledger.journal.domain.JournalHead;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.journal.domain.TransactionType;
import com.j4mb.ledger.journal.repository.JournalHeadRepository;
import com.j4mb.ledger.journal.repository.JournalLineRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.InsufficientBalanceException;
import com.j4mb.ledger.shared.exception.LedgerException;
import com.j4mb.ledger.shared.exception.UnbalancedJournalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalPostingServiceTest {

    @Mock JournalHeadRepository    journalHeadRepository;
    @Mock JournalLineRepository    journalLineRepository;
    @Mock FiscalPeriodService      fiscalPeriodService;
    @Mock DoubleEntryValidator     doubleEntryValidator;
    @Mock AuditService             auditService;
    @Mock UserContext               userContext;
    @Mock AccountRepository        accountRepository;
    @Mock CoaNodeRepository        coaNodeRepository;
    @Mock BalanceProjectionService balanceProjectionService;

    @InjectMocks JournalPostingService service;

    // ---- helpers --------------------------------------------------------

    private Account activeAccount(UUID coaNodeId) {
        return Account.create(coaNodeId, "ACC-001", "Test Account", "MYR", "test");
    }

    private CoaNode allowNegativeCoaNode() {
        // allowNegativeBalance defaults to true in the factory
        return CoaNode.create(null, "1000", "Assets", "1000", 0,
                AccountType.ASSET, NormalBalance.DEBIT, 1, "test");
    }

    // ---- existing tests -------------------------------------------------

    @Test
    void shouldPostJournal_whenBalancedAndPeriodOpen() {
        // Given
        UUID journalId  = UUID.randomUUID();
        UUID periodId   = UUID.randomUUID();
        UUID coaNodeId  = UUID.randomUUID();
        JournalHead head = JournalHead.create("JNL-001", TransactionType.SALE, periodId, "MYR", "Test", "test");

        Account account = activeAccount(coaNodeId);
        CoaNode coaNode = allowNegativeCoaNode();

        List<JournalLine> lines = List.of(
                JournalLine.create(journalId, (short) 1, account.getId(), EntryType.DEBIT,
                        new BigDecimal("1000.00"), "MYR", "MYR", new BigDecimal("1000.00"), BigDecimal.ONE, "test"),
                JournalLine.create(journalId, (short) 2, account.getId(), EntryType.CREDIT,
                        new BigDecimal("1000.00"), "MYR", "MYR", new BigDecimal("1000.00"), BigDecimal.ONE, "test")
        );

        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.of(head));
        when(journalLineRepository.findByJournalIdOrderByLineNumber(journalId)).thenReturn(lines);
        when(accountRepository.findAllById(any())).thenReturn(List.of(account));
        when(coaNodeRepository.findAllById(any())).thenReturn(List.of(coaNode));
        when(journalHeadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        JournalHead posted = service.post(journalId, "admin");

        // Then
        assertThat(posted.isPosted()).isTrue();
        assertThat(posted.getTotalDebit()).isEqualByComparingTo("1000.00");
        assertThat(posted.getTotalCredit()).isEqualByComparingTo("1000.00");
        assertThat(posted.getPostedBy()).isEqualTo("admin");
        verify(auditService).record("JOURNAL", posted.getId(), "POSTED", "admin", null, posted);
        verify(balanceProjectionService).applyJournal(any(), any(), any(), any());
    }

    @Test
    void shouldThrow_whenJournalNotFound() {
        // Given
        UUID journalId = UUID.randomUUID();
        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.post(journalId, "admin"))
                .isInstanceOf(LedgerException.class)
                .hasMessageContaining(journalId.toString());
    }

    @Test
    void shouldThrow_whenJournalAlreadyPosted() {
        // Given
        UUID journalId = UUID.randomUUID();
        UUID periodId  = UUID.randomUUID();
        JournalHead head = JournalHead.create("JNL-002", TransactionType.SALE, periodId, "MYR", "Already posted", "test");
        head.markPosted("someone");

        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.of(head));

        // When / Then
        assertThatThrownBy(() -> service.post(journalId, "admin"))
                .isInstanceOf(LedgerException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void shouldThrow_whenJournalUnbalanced() {
        // Given
        UUID journalId = UUID.randomUUID();
        UUID periodId  = UUID.randomUUID();
        JournalHead head = JournalHead.create("JNL-003", TransactionType.SALE, periodId, "MYR", "Unbalanced", "test");

        List<JournalLine> unbalancedLines = List.of(
                JournalLine.create(journalId, (short) 1, UUID.randomUUID(), EntryType.DEBIT,
                        new BigDecimal("500.00"), "MYR", "MYR", new BigDecimal("500.00"), BigDecimal.ONE, "test")
                // missing credit line
        );

        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.of(head));
        when(journalLineRepository.findByJournalIdOrderByLineNumber(journalId)).thenReturn(unbalancedLines);
        org.mockito.Mockito.doThrow(new UnbalancedJournalException(new BigDecimal("500.00"), BigDecimal.ZERO))
                .when(doubleEntryValidator).validate(unbalancedLines);

        // When / Then
        assertThatThrownBy(() -> service.post(journalId, "admin"))
                .isInstanceOf(UnbalancedJournalException.class);
    }

    // ---- new tests ------------------------------------------------------

    @Test
    void shouldThrow_whenAccountIsFrozen() {
        // Given
        UUID journalId = UUID.randomUUID();
        UUID periodId  = UUID.randomUUID();
        UUID coaNodeId = UUID.randomUUID();
        JournalHead head = JournalHead.create("JNL-004", TransactionType.SALE, periodId, "MYR", "Frozen account", "test");

        Account account = Account.create(coaNodeId, "ACC-002", "Frozen Account", "MYR", "test");
        account.freeze(); // ACTIVE -> FROZEN

        List<JournalLine> lines = List.of(
                JournalLine.create(journalId, (short) 1, account.getId(), EntryType.DEBIT,
                        new BigDecimal("500.00"), "MYR", "MYR", new BigDecimal("500.00"), BigDecimal.ONE, "test"),
                JournalLine.create(journalId, (short) 2, account.getId(), EntryType.CREDIT,
                        new BigDecimal("500.00"), "MYR", "MYR", new BigDecimal("500.00"), BigDecimal.ONE, "test")
        );

        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.of(head));
        when(journalLineRepository.findByJournalIdOrderByLineNumber(journalId)).thenReturn(lines);
        when(accountRepository.findAllById(any())).thenReturn(List.of(account));

        // When / Then
        assertThatThrownBy(() -> service.post(journalId, "admin"))
                .isInstanceOf(LedgerException.class)
                .hasMessageContaining("ACC-002");
    }

    @Test
    void shouldThrow_whenOverdraftBreached() {
        // Given
        UUID journalId = UUID.randomUUID();
        UUID periodId  = UUID.randomUUID();
        UUID coaNodeId = UUID.randomUUID();
        JournalHead head = JournalHead.create("JNL-005", TransactionType.SALE, periodId, "MYR", "Overdraft breach", "test");

        Account account = Account.create(coaNodeId, "ACC-003", "Restricted Account", "MYR", "test");

        CoaNode restrictedNode = CoaNode.create(null, "2000", "Liabilities", "2000", 0,
                AccountType.LIABILITY, NormalBalance.CREDIT, 2, "test");
        restrictedNode.disallowNegativeBalance();

        List<JournalLine> lines = List.of(
                JournalLine.create(journalId, (short) 1, account.getId(), EntryType.DEBIT,
                        new BigDecimal("500.00"), "MYR", "MYR", new BigDecimal("500.00"), BigDecimal.ONE, "test"),
                JournalLine.create(journalId, (short) 2, account.getId(), EntryType.CREDIT,
                        new BigDecimal("500.00"), "MYR", "MYR", new BigDecimal("500.00"), BigDecimal.ONE, "test")
        );

        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.of(head));
        when(journalLineRepository.findByJournalIdOrderByLineNumber(journalId)).thenReturn(lines);
        when(accountRepository.findAllById(any())).thenReturn(List.of(account));
        when(coaNodeRepository.findAllById(any())).thenReturn(List.of(restrictedNode));
        doThrow(new InsufficientBalanceException("ACC-003", BigDecimal.ZERO, new BigDecimal("500.00")))
                .when(balanceProjectionService).checkOverdraftAll(any(), any(), any(), any());

        // When / Then
        assertThatThrownBy(() -> service.post(journalId, "admin"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("ACC-003");
    }

    @Test
    void shouldCallApplyJournal_afterSuccessfulPost() {
        // Given
        UUID journalId = UUID.randomUUID();
        UUID periodId  = UUID.randomUUID();
        UUID coaNodeId = UUID.randomUUID();
        JournalHead head = JournalHead.create("JNL-006", TransactionType.ADJUSTMENT, periodId, "MYR", "Apply journal test", "test");

        Account account = activeAccount(coaNodeId);
        CoaNode coaNode = allowNegativeCoaNode();

        List<JournalLine> lines = List.of(
                JournalLine.create(journalId, (short) 1, account.getId(), EntryType.DEBIT,
                        new BigDecimal("200.00"), "MYR", "MYR", new BigDecimal("200.00"), BigDecimal.ONE, "test"),
                JournalLine.create(journalId, (short) 2, account.getId(), EntryType.CREDIT,
                        new BigDecimal("200.00"), "MYR", "MYR", new BigDecimal("200.00"), BigDecimal.ONE, "test")
        );

        when(journalHeadRepository.findById(journalId)).thenReturn(Optional.of(head));
        when(journalLineRepository.findByJournalIdOrderByLineNumber(journalId)).thenReturn(lines);
        when(accountRepository.findAllById(any())).thenReturn(List.of(account));
        when(coaNodeRepository.findAllById(any())).thenReturn(List.of(coaNode));
        when(journalHeadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.post(journalId, "admin");

        // Then
        verify(balanceProjectionService).applyJournal(any(), any(), any(), any());
    }
}
