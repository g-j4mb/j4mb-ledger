package com.j4mb.ledger.fiscal.repository;
import com.j4mb.ledger.fiscal.domain.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {
    List<FiscalPeriod> findByFiscalYearId(UUID fiscalYearId);
    Optional<FiscalPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate start, LocalDate end);
    List<FiscalPeriod> findByFiscalYearIdOrderByPeriodNumberAsc(UUID fiscalYearId);
    Optional<FiscalPeriod> findFirstByFiscalYearIdOrderByPeriodNumberAsc(UUID fiscalYearId);
}
