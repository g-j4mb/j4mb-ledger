package com.j4mb.ledger.account.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.domain.AccountStatus;
import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.journal.repository.JournalLineRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository     accountRepository;
    private final CoaNodeRepository     coaNodeRepository;
    private final JournalLineRepository journalLineRepository;
    private final UserContext           userContext;

    AccountService(AccountRepository accountRepository, CoaNodeRepository coaNodeRepository,
                   JournalLineRepository journalLineRepository, UserContext userContext) {
        this.accountRepository     = accountRepository;
        this.coaNodeRepository     = coaNodeRepository;
        this.journalLineRepository  = journalLineRepository;
        this.userContext           = userContext;
    }

    public Account findById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Account not found: " + id));
    }

    public Page<Account> listActive(Pageable pageable) {
        return accountRepository.findByStatus(AccountStatus.ACTIVE, pageable);
    }

    @Transactional
    public Account create(UUID coaNodeId, String accountNumber, String name, String currencyCode) {
        coaNodeRepository.findById(coaNodeId)
                .filter(n -> n.isPostable())
                .orElseThrow(() -> new LedgerException("COA node not found or not postable: " + coaNodeId));
        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new LedgerException("Account number already exists: " + accountNumber);
        }
        return accountRepository.save(
                Account.create(coaNodeId, accountNumber, name, currencyCode, userContext.getUserId()));
    }

    @Transactional
    public void freeze(UUID id) {
        findById(id).freeze();
    }

    @Transactional
    public void close(UUID id) {
        findById(id).close();
    }

    @Transactional
    public Account update(UUID id, String name, String description, BigDecimal overdraftLimit,
                          String externalRef) {
        Account account = findById(id);
        account.updateName(name);
        account.updateDescription(description);
        if (overdraftLimit != null) account.updateOverdraftLimit(overdraftLimit);
        account.updateExternalRef(externalRef);
        return accountRepository.save(account);
    }

    @Transactional
    public void delete(UUID id) {
        if (journalLineRepository.existsByAccountId(id)) {
            throw new LedgerException("Cannot delete account with journal history: " + id);
        }
        accountRepository.deleteById(id);
    }
}
