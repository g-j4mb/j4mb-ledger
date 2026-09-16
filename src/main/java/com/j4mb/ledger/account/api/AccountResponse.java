package com.j4mb.ledger.account.api;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.domain.AccountStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id, UUID coaNodeId, String accountNumber, String name,
        String currencyCode, AccountStatus status, BigDecimal overdraftLimit
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getCoaNodeId(), a.getAccountNumber(),
                a.getName(), a.getCurrencyCode(), a.getStatus(), a.getOverdraftLimit());
    }
}
