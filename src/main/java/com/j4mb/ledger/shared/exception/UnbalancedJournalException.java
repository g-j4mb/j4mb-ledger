package com.j4mb.ledger.shared.exception;
import java.math.BigDecimal;
public class UnbalancedJournalException extends LedgerException {
    public UnbalancedJournalException(BigDecimal debits, BigDecimal credits) {
        super("Journal unbalanced: debits=" + debits + ", credits=" + credits);
    }
}
