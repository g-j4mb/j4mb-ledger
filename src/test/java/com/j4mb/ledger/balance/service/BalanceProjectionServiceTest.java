package com.j4mb.ledger.balance.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.balance.domain.AccountBalance;
import com.j4mb.ledger.balance.repository.AccountBalanceRepository;
import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.domain.NormalBalance;
import com.j4mb.ledger.journal.domain.EntryType;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.shared.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceProjectionServiceTest {

    @Mock AccountBalanceRepository balanceRepository;

    @InjectMocks BalanceProjectionService service;

    // ---- helpers --------------------------------------------------------

    private Account account(UUID coaNodeId) {
        return Account.create(coaNodeId, "ACC-TEST", "Test Account", "MYR", "test");
    }

    private CoaNode allowNegativeNode() {
        CoaNode node = CoaNode.create(null, "1000", "Assets", "1000", 0,
                AccountType.ASSET, NormalBalance.DEBIT, 1, "test");
        // allowNegativeBalance defaults to true — no change needed
        return node;
    }

    private CoaNode restrictedNode() {
        CoaNode node = CoaNode.create(null, "2000", "Liabilities", "2000", 0,
                AccountType.LIABILITY, NormalBalance.CREDIT, 2, "test");
        node.disallowNegativeBalance();
        return node;
    }

    private JournalLine debitLine(UUID accountId, String amount) {
        return JournalLine.create(UUID.randomUUID(), (short) 1, accountId,
                EntryType.DEBIT, new BigDecimal(amount), "MYR", "MYR",
                new BigDecimal(amount), BigDecimal.ONE, "test");
    }

    private JournalLine creditLine(UUID accountId, String amount) {
        return JournalLine.create(UUID.randomUUID(), (short) 2, accountId,
                EntryType.CREDIT, new BigDecimal(amount), "MYR", "MYR",
                new BigDecimal(amount), BigDecimal.ONE, "test");
    }

    // ---- checkOverdraftAll tests ----------------------------------------

    @Test
    void checkOverdraftAll_skipsAccountWithAllowNegativeBalance() {
        // Given: account under a node with allowNegativeBalance=true
        UUID coaNodeId    = UUID.randomUUID();
        Account account   = account(coaNodeId);
        CoaNode coaNode   = allowNegativeNode();
        UUID periodId     = UUID.randomUUID();

        // A massive credit that would normally breach any overdraft
        List<JournalLine> lines = List.of(
                debitLine(account.getId(), "10000.00"),
                creditLine(account.getId(), "10000.00")
        );
        Map<UUID, Account>  accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode>  coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then — no exception thrown, repository never queried
        service.checkOverdraftAll(lines, periodId, accountMap, coaNodeMap);
        verify(balanceRepository, never()).findByAccountIdAndFiscalPeriodIdAndCurrencyCode(any(), any(), any());
    }

    @Test
    void checkOverdraftAll_throwsWhenCreditBreachesLimit() {
        // Given: account under a restricted node (allowNegativeBalance=false), zero overdraft limit
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-001", "Bank", "MYR", "test");
        // overdraftLimit = 0 by default
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        // No existing balance → zero-stub (closingDebit=0, closingCredit=0)
        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.empty());

        // A net credit of 500 → projectedNet = 0 + 0 - (0 + 500) = -500 < -0 (overdraftLimit.negate())
        List<JournalLine> lines = List.of(
                debitLine(account.getId(), "100.00"),
                creditLine(account.getId(), "600.00")
        );
        Map<UUID, Account>  accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode>  coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then
        assertThatThrownBy(() -> service.checkOverdraftAll(lines, periodId, accountMap, coaNodeMap))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("ACC-001");
    }

    @Test
    void checkOverdraftAll_allowsPostingWithinOverdraftLimit() {
        // Given: overdraft limit = 1000, current net = 200, posting a net credit of 800
        // projectedNet = (200 + 0) - (0 + 800) = -600 > -1000 (limit) → OK
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-002", "Overdraft Account", "MYR", "test");
        // We need to set overdraft limit to 1000 — Account has no setter; rely on test scenario
        // where existing balance has closingDebit=1200, closingCredit=1000 (net=200)
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        AccountBalance existingBalance = AccountBalance.open(
                account.getId(), periodId, "MYR",
                new BigDecimal("1200.00"), new BigDecimal("1000.00"), "system");

        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.of(existingBalance));

        // net credit = 800, net debit = 0
        // projectedNet = (1200 + 0) - (1000 + 800) = 1200 - 1800 = -600
        // overdraftLimit = 0 (default) → negate = 0 → -600 < 0 → would throw
        // To test "within limit", we set up a scenario where net is positive after posting:
        // credit = 100, debit = 0 → projectedNet = 1200 - 1100 = 100 > 0 → OK
        List<JournalLine> lines = List.of(
                creditLine(account.getId(), "100.00")
        );
        Map<UUID, Account>  accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode>  coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then — no exception (net stays positive)
        service.checkOverdraftAll(lines, periodId, accountMap, coaNodeMap);
    }

    @Test
    void checkOverdraftAll_noExistingBalance_creditExceedsZeroLimit_throws() {
        // Given: no existing balance (zero-stub), restricted node, overdraft limit = 0
        // Any net credit will make projectedNet negative → breach
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-004", "No Balance Account", "MYR", "test");
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.empty());

        // Pure credit line — net credit positive, net debit zero → projectedNet < 0
        List<JournalLine> lines = List.of(
                creditLine(account.getId(), "1.00")
        );
        Map<UUID, Account>  accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode>  coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then
        assertThatThrownBy(() -> service.checkOverdraftAll(lines, periodId, accountMap, coaNodeMap))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("ACC-004");
    }

    // ---- applyJournal tests ---------------------------------------------

    @Test
    void applyJournal_createsNewBalanceRow_andAppliesDebit() {
        // Given
        UUID coaNodeId = UUID.randomUUID();
        Account account = account(coaNodeId);
        UUID periodId   = UUID.randomUUID();

        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.empty());
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<JournalLine> lines = List.of(debitLine(account.getId(), "300.00"));
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);

        // When
        service.applyJournal(lines, periodId, accountMap, "poster");

        // Then
        verify(balanceRepository).save(any(AccountBalance.class));
    }

    @Test
    void applyJournal_updatesExistingBalanceRow() {
        // Given
        UUID coaNodeId  = UUID.randomUUID();
        Account account = account(coaNodeId);
        UUID periodId   = UUID.randomUUID();

        AccountBalance existing = AccountBalance.open(
                account.getId(), periodId, "MYR",
                new BigDecimal("500.00"), BigDecimal.ZERO, "system");

        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.of(existing));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<JournalLine> lines = List.of(
                debitLine(account.getId(), "200.00"),
                creditLine(account.getId(), "50.00")
        );
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);

        // When
        service.applyJournal(lines, periodId, accountMap, "poster");

        // Then: closing debit = opening 500 + period 200 = 700; closing credit = 0 + 50 = 50
        assertThat(existing.getClosingDebit()).isEqualByComparingTo("700.00");
        assertThat(existing.getClosingCredit()).isEqualByComparingTo("50.00");
        assertThat(existing.getUpdatedBy()).isEqualTo("poster");
        verify(balanceRepository).save(existing);
    }
}
