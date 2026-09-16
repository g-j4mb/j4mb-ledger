package com.j4mb.ledger.currency.repository;
import com.j4mb.ledger.currency.domain.ExchangeRate;
import com.j4mb.ledger.currency.domain.RateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    @Query("SELECT r FROM ExchangeRate r WHERE r.fromCurrency = :from AND r.toCurrency = :to AND r.rateType = :rateType AND r.effectiveDate <= :date ORDER BY r.effectiveDate DESC")
    Optional<ExchangeRate> findLatestRate(@Param("from") String from, @Param("to") String to,
                                           @Param("rateType") RateType rateType, @Param("date") LocalDate date);
}
