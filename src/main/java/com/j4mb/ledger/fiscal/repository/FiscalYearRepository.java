package com.j4mb.ledger.fiscal.repository;

import com.j4mb.ledger.fiscal.domain.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, UUID> {
    Optional<FiscalYear> findByYearName(String yearName);
    List<FiscalYear> findAllByOrderByStartDateDesc();
}
