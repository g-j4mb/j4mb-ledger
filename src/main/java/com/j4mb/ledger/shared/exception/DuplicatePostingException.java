package com.j4mb.ledger.shared.exception;
public class DuplicatePostingException extends LedgerException {
    public DuplicatePostingException(String sourceSystem, String sourceEventId) {
        super("Event already processed: system=" + sourceSystem + ", eventId=" + sourceEventId);
    }
}
