package com.j4mb.ledger.closing.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.audit.service.AuditService;
import com.j4mb.ledger.balance.domain.AccountBalance;
import com.j4mb.ledger.balance.repository.AccountBalanceRepository;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.fiscal.domain.FiscalPeriod;
import com.j4mb.ledger.fiscal.domain.FiscalYear;
import com.j4mb.ledger.fiscal.repository.FiscalPeriodRepository;
import com.j4mb.ledger.fiscal.repository.FiscalYearRepository;
import com.j4mb.ledger.journal.api.JournalLineRequest;
import com.j4mb.ledger.journal.domain.EntryType;
import com.j4mb.ledger.journal.domain.TransactionType;
import com.j4mb.ledger.journal.service.JournalService;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class YearEndClosingService {

    private static final Logger log = LoggerFactory.getLogger(YearEndClosingService.class);

    private final FiscalYearRepository     fiscalYearRepository;
    private final FiscalPeriodRepository   fiscalPeriodRepository;
    private final AccountRepository        accountRepository;
    private final AccountBalanceRepository balanceRepository;
    private final CoaNodeRepository        coaNodeRepository;
    private final JournalService           journalService;
    private final AuditService             auditService;
    private final UserContext              userContext;

    YearEndClosingService(FiscalYearRepository fiscalYearRepository,
                          FiscalPeriodRepository fiscalPeriodRepository,
                          AccountRepository accountRepository,
                          AccountBalanceRepository balanceRepository,
                          CoaNodeRepository coaNodeRepository,
                          JournalService journalService,
                          AuditService auditService,
                          UserContext userContext) {
        this.fiscalYearRepository   = fiscalYearRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountRepository      = accountRepository;
        this.balanceRepository      = balanceRepository;
        this.coaNodeRepository      = coaNodeRepository;
        this.journalService         = journalService;
        this.auditService           = auditService;
        this.userContext            = userContext;
    }

    @Transactional
    public FiscalYear closeYear(UUID fiscalYearId) {
        String closedBy = userContext.getUserId();

        FiscalYear year = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new LedgerException("Fiscal year not found: " + fiscalYearId));

        if (!year.isOpen())
            throw new LedgerException("Only OPEN fiscal years can be closed: " + fiscalYearId);

        // Assert all periods are CLOSED or LOCKED
        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNumberAsc(fiscalYearId);
        if (periods.isEmpty())
            throw new LedgerException("Fiscal year has no periods: " + fiscalYearId);

        boolean hasOpenPeriods = periods.stream().anyMatch(FiscalPeriod::isOpen);
        if (hasOpenPeriods)
            throw new LedgerException("All periods must be CLOSED or LOCKED before year-end close");

        // Find last period
        FiscalPeriod lastPeriod = periods.get(periods.size() - 1);

        // Load all accounts and their last-period balances
        List<Account> allAccounts = accountRepository.findAll();
        List<AccountBalance> lastPeriodBalances = balanceRepository.findByFiscalPeriodId(lastPeriod.getId());

        // Find INCOME_SUMMARY and RETAINED_EARNINGS accounts via COA node_role
        Account incomeSummaryAccount    = findAccountByNodeRole(allAccounts, "INCOME_SUMMARY");
        Account retainedEarningsAccount = findAccountByNodeRole(allAccounts, "RETAINED_EARNINGS");

        if (incomeSummaryAccount == null || retainedEarningsAccount == null) {
            throw new LedgerException(
                "Year-end close requires COA nodes with node_role='INCOME_SUMMARY' and node_role='RETAINED_EARNINGS' " +
                "with linked accounts");
        }

        String baseCurrency = getBaseCurrency(allAccounts);

        // Build closing journal lines: zero out REVENUE and EXPENSE accounts
        List<JournalLineRequest> closingLines = new ArrayList<>();
        BigDecimal netToIncomeSummary = BigDecimal.ZERO;

        for (AccountBalance balance : lastPeriodBalances) {
            Account account = allAccounts.stream()
                    .filter(a -> a.getId().equals(balance.getAccountId())).findFirst().orElse(null);
            if (account == null) continue;

            CoaNode coaNode = coaNodeRepository.findById(account.getCoaNodeId()).orElse(null);
            if (coaNode == null) continue;

            String type = coaNode.getAccountType().name();
            BigDecimal net = balance.getClosingCredit().subtract(balance.getClosingDebit());

            if ("REVENUE".equals(type) && net.compareTo(BigDecimal.ZERO) != 0) {
                // Revenue has credit balance — debit to zero it; net credit → Income Summary
                closingLines.add(new JournalLineRequest(account.getId(), EntryType.DEBIT,
                        net.abs(), account.getCurrencyCode(), "Year-end close: zero REVENUE"));
                netToIncomeSummary = netToIncomeSummary.add(net);
            } else if ("EXPENSE".equals(type)) {
                BigDecimal debitNet = balance.getClosingDebit().subtract(balance.getClosingCredit());
                if (debitNet.compareTo(BigDecimal.ZERO) != 0) {
                    // Expense has debit balance — credit to zero it; net debit → reduces Income Summary
                    closingLines.add(new JournalLineRequest(account.getId(), EntryType.CREDIT,
                            debitNet.abs(), account.getCurrencyCode(), "Year-end close: zero EXPENSE"));
                    netToIncomeSummary = netToIncomeSummary.subtract(debitNet);
                }
            }
        }

        if (!closingLines.isEmpty() && netToIncomeSummary.compareTo(BigDecimal.ZERO) != 0) {
            // Balance the closing journal against Income Summary
            if (netToIncomeSummary.compareTo(BigDecimal.ZERO) > 0) {
                closingLines.add(new JournalLineRequest(incomeSummaryAccount.getId(), EntryType.CREDIT,
                        netToIncomeSummary, baseCurrency, "Year-end: net to Income Summary"));
            } else {
                closingLines.add(new JournalLineRequest(incomeSummaryAccount.getId(), EntryType.DEBIT,
                        netToIncomeSummary.abs(), baseCurrency, "Year-end: net to Income Summary"));
            }

            var closingJournal = journalService.createDraft(lastPeriod.getId(), TransactionType.CLOSING,
                    baseCurrency, "Year-end closing: zero revenue/expense accounts",
                    year.getYearName(), "YEAR_END_CLOSE", fiscalYearId.toString(), closingLines);
            journalService.post(closingJournal.getId(), closedBy);
            log.info("Year-end closing journal posted: {}", closingJournal.getJournalNumber());
        }

        // Transfer Income Summary → Retained Earnings
        AccountBalance incomeSummaryBalance = balanceRepository
                .findByAccountIdAndFiscalPeriodIdAndCurrencyCode(
                        incomeSummaryAccount.getId(), lastPeriod.getId(), baseCurrency)
                .orElse(null);

        if (incomeSummaryBalance != null) {
            BigDecimal net = incomeSummaryBalance.getClosingCredit()
                    .subtract(incomeSummaryBalance.getClosingDebit());
            if (net.compareTo(BigDecimal.ZERO) != 0) {
                List<JournalLineRequest> transferLines = new ArrayList<>();
                if (net.compareTo(BigDecimal.ZERO) > 0) {
                    transferLines.add(new JournalLineRequest(incomeSummaryAccount.getId(), EntryType.DEBIT,
                            net, baseCurrency, "Transfer to Retained Earnings"));
                    transferLines.add(new JournalLineRequest(retainedEarningsAccount.getId(), EntryType.CREDIT,
                            net, baseCurrency, "Year-end retained earnings"));
                } else {
                    transferLines.add(new JournalLineRequest(incomeSummaryAccount.getId(), EntryType.CREDIT,
                            net.abs(), baseCurrency, "Transfer to Retained Earnings"));
                    transferLines.add(new JournalLineRequest(retainedEarningsAccount.getId(), EntryType.DEBIT,
                            net.abs(), baseCurrency, "Year-end retained earnings (loss)"));
                }
                var transferJournal = journalService.createDraft(lastPeriod.getId(), TransactionType.CLOSING,
                        baseCurrency, "Year-end: transfer Income Summary to Retained Earnings",
                        year.getYearName(), "YEAR_END_TRANSFER", fiscalYearId.toString(), transferLines);
                journalService.post(transferJournal.getId(), closedBy);
                log.info("Year-end transfer journal posted: {}", transferJournal.getJournalNumber());
            }
        }

        // Close fiscal year
        year.close(closedBy);
        FiscalYear closed = fiscalYearRepository.save(year);

        auditService.record("FISCAL_YEAR", fiscalYearId, "CLOSED", closedBy, null, closed);
        log.info("Fiscal year closed: {}", year.getYearName());

        return closed;
    }

    private Account findAccountByNodeRole(List<Account> accounts, String role) {
        return accounts.stream()
                .filter(a -> {
                    CoaNode node = coaNodeRepository.findById(a.getCoaNodeId()).orElse(null);
                    return node != null && role.equals(node.getNodeRole());
                })
                .findFirst().orElse(null);
    }

    private String getBaseCurrency(List<Account> accounts) {
        return accounts.stream()
                .map(Account::getCurrencyCode)
                .findFirst()
                .orElse("MYR");
    }
}
