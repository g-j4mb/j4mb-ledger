package com.j4mb.ledger.fiscal.service;

import com.j4mb.ledger.fiscal.domain.FiscalPeriod;
import com.j4mb.ledger.fiscal.domain.FiscalYear;
import com.j4mb.ledger.fiscal.repository.FiscalPeriodRepository;
import com.j4mb.ledger.fiscal.repository.FiscalYearRepository;
import com.j4mb.ledger.journal.repository.JournalHeadRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.ClosedPeriodException;
import com.j4mb.ledger.shared.exception.LedgerException;
import com.j4mb.ledger.shared.exception.LockedPeriodException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FiscalPeriodService {

    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final FiscalYearRepository   fiscalYearRepository;
    private final JournalHeadRepository  journalHeadRepository;
    private final UserContext            userContext;

    FiscalPeriodService(FiscalPeriodRepository fiscalPeriodRepository,
                        FiscalYearRepository fiscalYearRepository,
                        JournalHeadRepository journalHeadRepository,
                        UserContext userContext) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.fiscalYearRepository   = fiscalYearRepository;
        this.journalHeadRepository  = journalHeadRepository;
        this.userContext            = userContext;
    }

    public void assertPostable(UUID periodId) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new LedgerException("Fiscal period not found: " + periodId));
        if (period.isClosed()) throw new ClosedPeriodException(periodId);
        if (period.isLocked()) throw new LockedPeriodException(periodId);
    }

    public FiscalPeriod findById(UUID periodId) {
        return fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new LedgerException("Fiscal period not found: " + periodId));
    }

    public FiscalPeriod findForDate(LocalDate date) {
        return fiscalPeriodRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date)
                .orElseThrow(() -> new LedgerException("No fiscal period found for date: " + date));
    }

    public List<FiscalYear> listYears() {
        return fiscalYearRepository.findAllByOrderByStartDateDesc();
    }

    public List<FiscalPeriod> listPeriods(UUID fiscalYearId) {
        return fiscalPeriodRepository.findByFiscalYearId(fiscalYearId);
    }

    @Transactional
    public FiscalYear createYear(String yearName, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new LedgerException("Start date must be before end date");
        }
        if (fiscalYearRepository.findByYearName(yearName).isPresent()) {
            throw new LedgerException("Fiscal year already exists: " + yearName);
        }
        return fiscalYearRepository.save(FiscalYear.create(yearName, startDate, endDate, userContext.getUserId()));
    }

    @Transactional
    public FiscalPeriod createPeriod(UUID fiscalYearId, String periodName, short periodNumber,
                                      LocalDate startDate, LocalDate endDate) {
        fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new LedgerException("Fiscal year not found: " + fiscalYearId));
        return fiscalPeriodRepository.save(
                FiscalPeriod.create(fiscalYearId, periodName, periodNumber, startDate, endDate,
                        userContext.getUserId()));
    }

    @Transactional
    public FiscalPeriod closePeriod(UUID periodId, String closedBy) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new LedgerException("Fiscal period not found: " + periodId));
        period.close(closedBy);
        return fiscalPeriodRepository.save(period);
    }

    @Transactional
    public FiscalPeriod reopenPeriod(UUID periodId, String reopenedBy) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new LedgerException("Fiscal period not found: " + periodId));
        period.reopen(reopenedBy);
        return fiscalPeriodRepository.save(period);
    }

    @Transactional
    public FiscalPeriod lockPeriod(UUID periodId, String lockedBy) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new LedgerException("Fiscal period not found: " + periodId));
        period.lock(lockedBy);
        return fiscalPeriodRepository.save(period);
    }

    @Transactional
    public FiscalYear updateYear(UUID id, String yearName, LocalDate startDate, LocalDate endDate) {
        FiscalYear year = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Fiscal year not found: " + id));
        if (!year.isOpen())
            throw new LedgerException("Only OPEN fiscal years can be updated");
        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearId(id);
        List<UUID> periodIds = periods.stream().map(FiscalPeriod::getId).toList();
        if (!periodIds.isEmpty() && journalHeadRepository.existsByFiscalPeriodIdIn(periodIds))
            throw new LedgerException("Cannot update fiscal year with posted journals");
        year.updateYearName(yearName);
        year.updateStartDate(startDate);
        year.updateEndDate(endDate);
        return fiscalYearRepository.save(year);
    }

    @Transactional
    public void deleteYear(UUID id) {
        FiscalYear year = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Fiscal year not found: " + id));
        if (!year.isOpen())
            throw new LedgerException("Only OPEN fiscal years can be deleted");
        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearId(id);
        if (!periods.isEmpty())
            throw new LedgerException("Cannot delete fiscal year with periods");
        fiscalYearRepository.deleteById(id);
    }

    @Transactional
    public FiscalPeriod updatePeriod(UUID id, String periodName, LocalDate startDate, LocalDate endDate) {
        FiscalPeriod period = fiscalPeriodRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Period not found: " + id));
        if (!period.isOpen())
            throw new LedgerException("Only OPEN periods can be updated");
        if (journalHeadRepository.existsByFiscalPeriodIdAndStatus(id,
                com.j4mb.ledger.journal.domain.JournalStatus.POSTED))
            throw new LedgerException("Cannot update period with posted journals");
        period.updatePeriodName(periodName);
        period.updateStartDate(startDate);
        period.updateEndDate(endDate);
        return fiscalPeriodRepository.save(period);
    }

    @Transactional
    public void deletePeriod(UUID id) {
        FiscalPeriod period = fiscalPeriodRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Period not found: " + id));
        if (!period.isOpen())
            throw new LedgerException("Only OPEN periods can be deleted");
        if (journalHeadRepository.existsByFiscalPeriodId(id))
            throw new LedgerException("Cannot delete period with journals");
        fiscalPeriodRepository.deleteById(id);
    }
}
