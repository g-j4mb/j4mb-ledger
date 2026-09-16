package com.j4mb.ledger.balance.repository;
import com.j4mb.ledger.balance.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    Optional<AccountBalance> findByAccountIdAndFiscalPeriodIdAndCurrencyCode(UUID accountId, UUID periodId, String currency);
    List<AccountBalance> findByFiscalPeriodId(UUID periodId);
}
