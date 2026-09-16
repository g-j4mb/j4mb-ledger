package com.j4mb.ledger.closing.service;

import com.j4mb.ledger.audit.service.AuditService;
import com.j4mb.ledger.fiscal.domain.FiscalPeriod;
import com.j4mb.ledger.fiscal.service.FiscalPeriodService;
import com.j4mb.ledger.journal.domain.JournalStatus;
import com.j4mb.ledger.journal.repository.JournalHeadRepository;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PeriodClosingService {

    private final FiscalPeriodService    fiscalPeriodService;
    private final JournalHeadRepository  journalHeadRepository;
    private final AuditService           auditService;

    PeriodClosingService(FiscalPeriodService fiscalPeriodService,
                         JournalHeadRepository journalHeadRepository,
                         AuditService auditService) {
        this.fiscalPeriodService   = fiscalPeriodService;
        this.journalHeadRepository = journalHeadRepository;
        this.auditService          = auditService;
    }

    @Transactional
    public FiscalPeriod closePeriod(UUID periodId, String closedBy) {
        FiscalPeriod period = fiscalPeriodService.findById(periodId);
        if (!period.isOpen())
            throw new LedgerException("Only OPEN periods can be closed: " + periodId);

        // Guard: no DRAFT journals in this period
        if (journalHeadRepository.existsByFiscalPeriodIdAndStatus(periodId, JournalStatus.DRAFT)) {
            throw new LedgerException(
                "Cannot close period with outstanding DRAFT journals — post or cancel them first");
        }

        FiscalPeriod closed = fiscalPeriodService.closePeriod(periodId, closedBy);
        auditService.record("FISCAL_PERIOD", periodId, "CLOSED", closedBy, null, closed);
        return closed;
    }

    @Transactional
    public FiscalPeriod reopenPeriod(UUID periodId, String reopenedBy) {
        FiscalPeriod period = fiscalPeriodService.findById(periodId);
        if (!period.isClosed())
            throw new LedgerException("Only CLOSED periods can be reopened: " + periodId);

        FiscalPeriod reopened = fiscalPeriodService.reopenPeriod(periodId, reopenedBy);
        auditService.record("FISCAL_PERIOD", periodId, "REOPENED", reopenedBy, null, reopened);
        return reopened;
    }

    @Transactional
    public FiscalPeriod lockPeriod(UUID periodId, String lockedBy) {
        FiscalPeriod period = fiscalPeriodService.findById(periodId);
        if (!period.isClosed())
            throw new LedgerException("Only CLOSED periods can be locked: " + periodId);

        FiscalPeriod locked = fiscalPeriodService.lockPeriod(periodId, lockedBy);
        auditService.record("FISCAL_PERIOD", periodId, "LOCKED", lockedBy, null, locked);
        return locked;
    }
}
