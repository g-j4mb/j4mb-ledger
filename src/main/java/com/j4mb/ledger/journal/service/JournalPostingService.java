package com.j4mb.ledger.journal.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.audit.service.AuditService;
import com.j4mb.ledger.balance.service.BalanceProjectionService;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.fiscal.service.FiscalPeriodService;
import com.j4mb.ledger.journal.domain.JournalHead;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.journal.repository.JournalHeadRepository;
import com.j4mb.ledger.journal.repository.JournalLineRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JournalPostingService {

    private static final Logger log = LoggerFactory.getLogger(JournalPostingService.class);

    private final JournalHeadRepository    journalHeadRepository;
    private final JournalLineRepository    journalLineRepository;
    private final FiscalPeriodService      fiscalPeriodService;
    private final DoubleEntryValidator     doubleEntryValidator;
    private final AuditService             auditService;
    private final UserContext              userContext;
    private final AccountRepository        accountRepository;
    private final CoaNodeRepository        coaNodeRepository;
    private final BalanceProjectionService balanceProjectionService;

    JournalPostingService(JournalHeadRepository journalHeadRepository,
                          JournalLineRepository journalLineRepository,
                          FiscalPeriodService fiscalPeriodService,
                          DoubleEntryValidator doubleEntryValidator,
                          AuditService auditService,
                          UserContext userContext,
                          AccountRepository accountRepository,
                          CoaNodeRepository coaNodeRepository,
                          BalanceProjectionService balanceProjectionService) {
        this.journalHeadRepository    = journalHeadRepository;
        this.journalLineRepository    = journalLineRepository;
        this.fiscalPeriodService      = fiscalPeriodService;
        this.doubleEntryValidator     = doubleEntryValidator;
        this.auditService             = auditService;
        this.userContext              = userContext;
        this.accountRepository        = accountRepository;
        this.coaNodeRepository        = coaNodeRepository;
        this.balanceProjectionService = balanceProjectionService;
    }

    @Transactional
    public JournalHead post(UUID journalId, String postedBy) {
        // 1. Load journal
        JournalHead journal = journalHeadRepository.findById(journalId)
                .orElseThrow(() -> new LedgerException("Journal not found: " + journalId));

        // 2. Assert DRAFT
        if (!journal.isDraft()) {
            throw new LedgerException("Only DRAFT journals can be posted: " + journalId);
        }

        // 3. Assert period postable (not CLOSED or LOCKED)
        fiscalPeriodService.assertPostable(journal.getFiscalPeriodId());

        // 4. Load and validate lines
        List<JournalLine> lines = journalLineRepository.findByJournalIdOrderByLineNumber(journalId);
        doubleEntryValidator.validate(lines);

        // 5. Load accounts in bulk
        List<UUID> accountIds = lines.stream().map(JournalLine::getAccountId).distinct().toList();
        Map<UUID, Account> accountMap = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        // 6. Assert all accounts ACTIVE
        accountMap.values().forEach(account -> {
            if (!account.isActive()) {
                throw new LedgerException("Account is not ACTIVE: " + account.getAccountNumber());
            }
        });

        // 7. Load COA nodes in bulk
        List<UUID> coaNodeIds = accountMap.values().stream()
                .map(Account::getCoaNodeId).distinct().toList();
        Map<UUID, CoaNode> coaNodeMap = coaNodeRepository.findAllById(coaNodeIds).stream()
                .collect(Collectors.toMap(CoaNode::getId, n -> n));

        // 8. Compute totals
        BigDecimal totalDebit  = lines.stream().filter(JournalLine::isDebit)
                .map(JournalLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().filter(JournalLine::isCredit)
                .map(JournalLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 9. Post
        journal.updateTotals(totalDebit, totalCredit);
        journal.markPosted(postedBy);
        JournalHead saved = journalHeadRepository.save(journal);

        // 10. Validate overdraft limits and apply balance projection atomically (per account,
        // in canonical accountId order — see BalanceProjectionService.applyJournal for why)
        balanceProjectionService.applyJournal(lines, journal.getFiscalPeriodId(), accountMap, coaNodeMap, postedBy);

        // 11. Audit
        auditService.record("JOURNAL", saved.getId(), "POSTED", postedBy, null, saved);

        log.info("Journal posted: journalId={}, number={}, debit={}, credit={}",
                saved.getId(), saved.getJournalNumber(), totalDebit, totalCredit);

        return saved;
    }
}
