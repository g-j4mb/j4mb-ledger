package com.j4mb.ledger.shared.exception;
import java.util.UUID;
public class LockedPeriodException extends LedgerException {
    public LockedPeriodException(UUID periodId) {
        super("Fiscal period is locked and cannot accept postings: " + periodId);
    }
}
