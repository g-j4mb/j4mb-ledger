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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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

    // ---- applyJournal: allowNegativeBalance accounts skip the limit check ----

    @Test
    void applyJournal_unconditionalUpdate_forAccountWithAllowNegativeBalance() {
        // Given: account under a node with allowNegativeBalance=true
        UUID coaNodeId = UUID.randomUUID();
        Account account = account(coaNodeId);
        CoaNode coaNode = allowNegativeNode();
        UUID periodId   = UUID.randomUUID();

        when(balanceRepository.applyDelta(eq(account.getId()), eq(periodId), eq("MYR"),
                any(), any(), anyString(), any(Instant.class))).thenReturn(1);

        // A massive credit that would normally breach any overdraft
        List<JournalLine> lines = List.of(
                debitLine(account.getId(), "10000.00"),
                creditLine(account.getId(), "10000.00")
        );
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode> coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then — the unconditional (no-limit) update path is used, never the limited one
        service.applyJournal(lines, periodId, accountMap, coaNodeMap, "poster");
        verify(balanceRepository, never()).applyDeltaIfWithinLimit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---- applyJournal: overdraft enforcement via the conditional UPDATE ----

    @Test
    void applyJournal_throwsWhenCreditBreachesLimit() {
        // Given: account under a restricted node (allowNegativeBalance=false), zero overdraft limit
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-001", "Bank", "MYR", "test");
        // overdraftLimit = 0 by default
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        // Conditional update affects 0 rows (limit breached), and a balance row already exists
        when(balanceRepository.applyDeltaIfWithinLimit(eq(account.getId()), eq(periodId), eq("MYR"),
                any(), any(), any(), anyString(), any(Instant.class))).thenReturn(0);
        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.of(AccountBalance.open(
                        account.getId(), periodId, "MYR", BigDecimal.ZERO, BigDecimal.ZERO, "system")));

        List<JournalLine> lines = List.of(
                debitLine(account.getId(), "100.00"),
                creditLine(account.getId(), "600.00")
        );
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode> coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then
        assertThatThrownBy(() -> service.applyJournal(lines, periodId, accountMap, coaNodeMap, "poster"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("ACC-001");
    }

    @Test
    void applyJournal_allowsPostingWithinOverdraftLimit() {
        // Given: the conditional UPDATE succeeds (limit not breached)
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-002", "Overdraft Account", "MYR", "test");
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        when(balanceRepository.applyDeltaIfWithinLimit(eq(account.getId()), eq(periodId), eq("MYR"),
                any(), any(), any(), anyString(), any(Instant.class))).thenReturn(1);

        List<JournalLine> lines = List.of(
                creditLine(account.getId(), "100.00")
        );
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode> coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then — no exception
        service.applyJournal(lines, periodId, accountMap, coaNodeMap, "poster");
        verify(balanceRepository, never()).findByAccountIdAndFiscalPeriodIdAndCurrencyCode(any(), any(), any());
    }

    @Test
    void applyJournal_noExistingBalanceRow_createsItThenRetriesUpdate() {
        // Given: first conditional UPDATE affects 0 rows because no balance row exists yet
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-004", "No Balance Account", "MYR", "test");
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        when(balanceRepository.applyDeltaIfWithinLimit(eq(account.getId()), eq(periodId), eq("MYR"),
                any(), any(), any(), anyString(), any(Instant.class)))
                .thenReturn(0)  // first attempt: row doesn't exist
                .thenReturn(1); // retry after creation: succeeds
        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.empty());
        when(balanceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        List<JournalLine> lines = List.of(debitLine(account.getId(), "1.00"));
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode> coaNodeMap = Map.of(coaNodeId, coaNode);

        // When
        service.applyJournal(lines, periodId, accountMap, coaNodeMap, "poster");

        // Then
        verify(balanceRepository).saveAndFlush(any(AccountBalance.class));
    }

    @Test
    void applyJournal_noExistingBalance_creditExceedsZeroLimit_throws() {
        // Given: row gets created (zero-stub), but the retried conditional UPDATE still fails
        // because a pure credit against a zero balance breaches the zero overdraft limit
        UUID coaNodeId  = UUID.randomUUID();
        Account account = Account.create(coaNodeId, "ACC-004", "No Balance Account", "MYR", "test");
        CoaNode coaNode = restrictedNode();
        UUID periodId   = UUID.randomUUID();

        when(balanceRepository.applyDeltaIfWithinLimit(eq(account.getId()), eq(periodId), eq("MYR"),
                any(), any(), any(), anyString(), any(Instant.class))).thenReturn(0);
        when(balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                account.getId(), periodId, "MYR"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(AccountBalance.open(
                        account.getId(), periodId, "MYR", BigDecimal.ZERO, BigDecimal.ZERO, "poster")));
        when(balanceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        List<JournalLine> lines = List.of(creditLine(account.getId(), "1.00"));
        Map<UUID, Account> accountMap = Map.of(account.getId(), account);
        Map<UUID, CoaNode> coaNodeMap = Map.of(coaNodeId, coaNode);

        // When / Then
        assertThatThrownBy(() -> service.applyJournal(lines, periodId, accountMap, coaNodeMap, "poster"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("ACC-004");
    }

    @Test
    void applyJournal_processesAccountsInCanonicalUuidOrder() {
        // Given: two accounts, deliberately referenced in the journal's lines in the
        // opposite order to their UUID sort order
        UUID coaNodeId = UUID.randomUUID();
        CoaNode coaNode = allowNegativeNode();
        UUID periodId  = UUID.randomUUID();

        Account lowId  = account(coaNodeId);
        Account highId = account(coaNodeId);
        // Force a deterministic ordering regardless of random UUID generation
        Account first  = lowId.getId().compareTo(highId.getId()) < 0 ? lowId : highId;
        Account second = first == lowId ? highId : lowId;

        when(balanceRepository.applyDelta(any(), eq(periodId), eq("MYR"), any(), any(), anyString(), any(Instant.class)))
                .thenReturn(1);

        // Lines list the "second" (higher UUID) account first, "first" (lower UUID) second —
        // canonical processing order must still be first, then second.
        List<JournalLine> lines = List.of(
                debitLine(second.getId(), "50.00"),
                debitLine(first.getId(), "50.00")
        );
        Map<UUID, Account> accountMap = Map.of(first.getId(), first, second.getId(), second);
        Map<UUID, CoaNode> coaNodeMap = Map.of(coaNodeId, coaNode);

        // When
        service.applyJournal(lines, periodId, accountMap, coaNodeMap, "poster");

        // Then: the lower-UUID account is always updated before the higher-UUID one,
        // regardless of the order the journal's lines referenced them in
        InOrder order = inOrder(balanceRepository);
        order.verify(balanceRepository).applyDelta(eq(first.getId()), eq(periodId), eq("MYR"), any(), any(), anyString(), any(Instant.class));
        order.verify(balanceRepository).applyDelta(eq(second.getId()), eq(periodId), eq("MYR"), any(), any(), anyString(), any(Instant.class));
    }
}
