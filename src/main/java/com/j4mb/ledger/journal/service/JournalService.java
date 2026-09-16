package com.j4mb.ledger.journal.service;

import com.j4mb.ledger.audit.service.AuditService;
import com.j4mb.ledger.journal.api.JournalLineRequest;
import com.j4mb.ledger.journal.domain.JournalHead;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.journal.domain.TransactionType;
import com.j4mb.ledger.journal.repository.JournalHeadRepository;
import com.j4mb.ledger.journal.repository.JournalLineRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JournalService {

    private final JournalHeadRepository journalHeadRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalPostingService journalPostingService;
    private final AuditService          auditService;
    private final UserContext           userContext;

    JournalService(JournalHeadRepository journalHeadRepository,
                   JournalLineRepository journalLineRepository,
                   JournalPostingService journalPostingService,
                   AuditService auditService,
                   UserContext userContext) {
        this.journalHeadRepository = journalHeadRepository;
        this.journalLineRepository  = journalLineRepository;
        this.journalPostingService  = journalPostingService;
        this.auditService           = auditService;
        this.userContext            = userContext;
    }

    public JournalHead findById(UUID id) {
        return journalHeadRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Journal not found: " + id));
    }

    public List<JournalLine> findLines(UUID journalId) {
        return journalLineRepository.findByJournalIdOrderByLineNumber(journalId);
    }

    @Transactional
    public JournalHead createDraft(UUID fiscalPeriodId, TransactionType type,
                                    String currencyCode, String description, String reference,
                                    String sourceSystem, String sourceDocumentId,
                                    List<JournalLineRequest> lineRequests) {
        String journalNumber = generateJournalNumber();
        String actorId = userContext.getUserId();
        JournalHead head = JournalHead.create(journalNumber, type, fiscalPeriodId, currencyCode,
                description, actorId);
        head = journalHeadRepository.save(head);

        final UUID journalId = head.getId();
        short lineNum = 1;
        for (JournalLineRequest req : lineRequests) {
            JournalLine line = JournalLine.create(
                    journalId, lineNum++, req.accountId(),
                    req.entryType(), req.amount(),
                    req.currencyCode(), currencyCode,
                    req.amount(),      // base_amount = amount when same currency; FX not implemented yet
                    BigDecimal.ONE,    // exchange_rate = 1; FX rates resolved in currency domain later
                    actorId);
            journalLineRepository.save(line);
        }

        return head;
    }

    @Transactional
    public JournalHead post(UUID journalId, String postedBy) {
        return journalPostingService.post(journalId, postedBy);
    }

    @Transactional
    public JournalHead updateDraft(UUID journalId, String description, String reference) {
        JournalHead head = findById(journalId);
        if (!head.isDraft())
            throw new LedgerException("Only DRAFT journals can be updated");
        head.updateDescription(description);
        head.updateReference(reference);
        return journalHeadRepository.save(head);
    }

    @Transactional
    public JournalLine addLine(UUID journalId, JournalLineRequest req) {
        JournalHead head = findById(journalId);
        if (!head.isDraft())
            throw new LedgerException("Lines can only be added to DRAFT journals");
        short nextLineNumber = (short) (journalLineRepository.findByJournalIdOrderByLineNumber(journalId).size() + 1);
        JournalLine line = JournalLine.create(journalId, nextLineNumber, req.accountId(),
                req.entryType(), req.amount(), req.currencyCode(), head.getCurrencyCode(),
                req.amount(), BigDecimal.ONE, userContext.getUserId());
        return journalLineRepository.save(line);
    }

    @Transactional
    public void removeLine(UUID journalId, UUID lineId) {
        JournalHead head = findById(journalId);
        if (!head.isDraft())
            throw new LedgerException("Lines can only be removed from DRAFT journals");
        JournalLine line = journalLineRepository.findByJournalIdAndId(journalId, lineId)
                .orElseThrow(() -> new LedgerException("Journal line not found: " + lineId));
        journalLineRepository.delete(line);
    }

    @Transactional
    public void cancel(UUID journalId) {
        JournalHead head = journalHeadRepository.findById(journalId)
                .orElseThrow(() -> new LedgerException("Journal not found: " + journalId));
        head.cancel();
        journalHeadRepository.save(head);
        auditService.record("JOURNAL", journalId, "CANCELLED", userContext.getUserId(), head, null);
    }

    private String generateJournalNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "JNL-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
