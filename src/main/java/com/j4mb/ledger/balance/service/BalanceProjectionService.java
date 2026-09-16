package com.j4mb.ledger.balance.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.balance.domain.AccountBalance;
import com.j4mb.ledger.balance.repository.AccountBalanceRepository;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.shared.exception.InsufficientBalanceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BalanceProjectionService {

    private final AccountBalanceRepository balanceRepository;

    BalanceProjectionService(AccountBalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    /**
     * Checks overdraft limits for all accounts in the journal that have
     * allowNegativeBalance=false on their COA node.
     * Throws InsufficientBalanceException on the first breach found.
     */
    public void checkOverdraftAll(List<JournalLine> lines,
                                   UUID fiscalPeriodId,
                                   Map<UUID, Account> accountMap,
                                   Map<UUID, CoaNode> coaNodeMap) {

        // Group lines by accountId
        Map<UUID, List<JournalLine>> byAccount = lines.stream()
                .collect(Collectors.groupingBy(JournalLine::getAccountId));

        for (Map.Entry<UUID, List<JournalLine>> entry : byAccount.entrySet()) {
            UUID accountId  = entry.getKey();
            Account account = accountMap.get(accountId);
            CoaNode coaNode = coaNodeMap.get(account.getCoaNodeId());

            // Skip accounts where negative balance is allowed
            if (coaNode == null || coaNode.isAllowNegativeBalance()) continue;

            List<JournalLine> accountLines = entry.getValue();

            BigDecimal netDebit  = accountLines.stream()
                    .filter(JournalLine::isDebit)
                    .map(JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netCredit = accountLines.stream()
                    .filter(JournalLine::isCredit)
                    .map(JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Load current balance or use zero-stub (not persisted)
            AccountBalance balance = balanceRepository
                    .findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                            accountId, fiscalPeriodId, account.getCurrencyCode())
                    .orElse(AccountBalance.open(accountId, fiscalPeriodId,
                            account.getCurrencyCode(), BigDecimal.ZERO, BigDecimal.ZERO, "system"));

            BigDecimal projectedNet = balance.getClosingDebit().add(netDebit)
                    .subtract(balance.getClosingCredit().add(netCredit));

            BigDecimal overdraftLimit = account.getOverdraftLimit();
            if (projectedNet.compareTo(overdraftLimit.negate()) < 0) {
                BigDecimal currentNet = balance.getClosingDebit().subtract(balance.getClosingCredit());
                BigDecimal available  = currentNet.add(overdraftLimit);
                BigDecimal required   = netCredit.subtract(netDebit); // net credit change (reduces balance)
                throw new InsufficientBalanceException(account.getAccountNumber(), available, required);
            }
        }
    }

    /**
     * Applies all journal lines to their respective account balances.
     * Creates a new AccountBalance row if none exists for this account+period.
     * Uses @Version optimistic locking — concurrent updates throw ObjectOptimisticLockingFailureException.
     */
    @Transactional
    public void applyJournal(List<JournalLine> lines,
                              UUID fiscalPeriodId,
                              Map<UUID, Account> accountMap,
                              String appliedBy) {

        Map<UUID, List<JournalLine>> byAccount = lines.stream()
                .collect(Collectors.groupingBy(JournalLine::getAccountId));

        for (Map.Entry<UUID, List<JournalLine>> entry : byAccount.entrySet()) {
            UUID accountId  = entry.getKey();
            Account account = accountMap.get(accountId);

            AccountBalance balance = balanceRepository
                    .findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                            accountId, fiscalPeriodId, account.getCurrencyCode())
                    .orElse(AccountBalance.open(accountId, fiscalPeriodId,
                            account.getCurrencyCode(), BigDecimal.ZERO, BigDecimal.ZERO, appliedBy));

            for (JournalLine line : entry.getValue()) {
                if (line.isDebit())  balance.applyDebit(line.getAmount());
                if (line.isCredit()) balance.applyCredit(line.getAmount());
            }
            balance.setUpdatedBy(appliedBy);
            balanceRepository.save(balance);
        }
    }
}
