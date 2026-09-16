package com.j4mb.ledger.shared.exception;

import java.util.UUID;

public class ClosedPeriodException extends LedgerException {
    public ClosedPeriodException(UUID periodId) {
        super("Fiscal period is closed — reopen it before posting: " + periodId);
    }
}
