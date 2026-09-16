package com.j4mb.ledger.account.repository;
import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.domain.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);
    boolean existsByCoaNodeId(UUID coaNodeId);
}
