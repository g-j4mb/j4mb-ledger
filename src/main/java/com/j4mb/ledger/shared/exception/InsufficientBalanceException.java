package com.j4mb.ledger.shared.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends LedgerException {

    public InsufficientBalanceException(String accountNumber, BigDecimal available, BigDecimal required) {
        super("Insufficient balance on account " + accountNumber
                + ": available=" + available.toPlainString()
                + ", required=" + required.toPlainString());
    }
}
