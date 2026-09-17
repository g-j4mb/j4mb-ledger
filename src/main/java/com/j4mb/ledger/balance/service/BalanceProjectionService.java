package com.j4mb.ledger.balance.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.balance.domain.AccountBalance;
import com.j4mb.ledger.balance.repository.AccountBalanceRepository;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.shared.exception.InsufficientBalanceException;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BalanceProjectionService {

    private final AccountBalanceRepository balanceRepository;

    BalanceProjectionService(AccountBalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    /**
     * Validates the overdraft limit and applies all journal lines to their respective
     * account balances in one atomic step per account — the check and the mutation are
     * the same conditional UPDATE, closing the check-then-apply race that a separate
     * read-validate-write sequence would leave open.
     * <p>
     * Accounts are processed in a canonical order (sorted by accountId) rather than the
     * order they appear in the journal's lines, so that any two journals sharing two or
     * more accounts always touch those rows in the same relative order — this is what
     * prevents a deadlock when, e.g., one journal debits A/credits B while a concurrent
     * journal debits B/credits A.
     */
    @Transactional
    public void applyJournal(List<JournalLine> lines,
                              UUID fiscalPeriodId,
                              Map<UUID, Account> accountMap,
                              Map<UUID, CoaNode> coaNodeMap,
                              String appliedBy) {

        Map<UUID, List<JournalLine>> byAccount = lines.stream()
                .collect(Collectors.groupingBy(JournalLine::getAccountId));

        List<UUID> orderedAccountIds = new ArrayList<>(byAccount.keySet());
        Collections.sort(orderedAccountIds);

        Instant now = Instant.now();

        for (UUID accountId : orderedAccountIds) {
            Account account = accountMap.get(accountId);
            CoaNode coaNode = coaNodeMap.get(account.getCoaNodeId());
            List<JournalLine> accountLines = byAccount.get(accountId);

            BigDecimal netDebit  = accountLines.stream()
                    .filter(JournalLine::isDebit)
                    .map(JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netCredit = accountLines.stream()
                    .filter(JournalLine::isCredit)
                    .map(JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean enforceLimit = coaNode != null && !coaNode.isAllowNegativeBalance();

            int updated = applyDelta(accountId, fiscalPeriodId, account, netDebit, netCredit, enforceLimit, appliedBy, now);

            if (updated == 0) {
                // No balance row yet for this account+period+currency — create the zero-stub, then retry.
                if (balanceRepository.findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                        accountId, fiscalPeriodId, account.getCurrencyCode()).isEmpty()) {
                    try {
                        balanceRepository.saveAndFlush(AccountBalance.open(accountId, fiscalPeriodId,
                                account.getCurrencyCode(), BigDecimal.ZERO, BigDecimal.ZERO, appliedBy));
                    } catch (DataIntegrityViolationException raceLost) {
                        // A concurrent journal created it first (unique constraint) — fall through and retry.
                    }
                    updated = applyDelta(accountId, fiscalPeriodId, account, netDebit, netCredit, enforceLimit, appliedBy, now);
                }
            }

            if (updated == 0) {
                AccountBalance current = balanceRepository
                        .findByAccountIdAndFiscalPeriodIdAndCurrencyCode(accountId, fiscalPeriodId, account.getCurrencyCode())
                        .orElseThrow(() -> new LedgerException("Balance row missing for account: " + account.getAccountNumber()));
                BigDecimal currentNet = current.getClosingDebit().subtract(current.getClosingCredit());
                BigDecimal available  = currentNet.add(account.getOverdraftLimit());
                BigDecimal required   = netCredit.subtract(netDebit); // net credit change (reduces balance)
                throw new InsufficientBalanceException(account.getAccountNumber(), available, required);
            }
        }
    }

    private int applyDelta(UUID accountId, UUID fiscalPeriodId, Account account,
                            BigDecimal netDebit, BigDecimal netCredit, boolean enforceLimit,
                            String appliedBy, Instant now) {
        return enforceLimit
                ? balanceRepository.applyDeltaIfWithinLimit(accountId, fiscalPeriodId, account.getCurrencyCode(),
                        netDebit, netCredit, account.getOverdraftLimit().negate(), appliedBy, now)
                : balanceRepository.applyDelta(accountId, fiscalPeriodId, account.getCurrencyCode(),
                        netDebit, netCredit, appliedBy, now);
    }
}
