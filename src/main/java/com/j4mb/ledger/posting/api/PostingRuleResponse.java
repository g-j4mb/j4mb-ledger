package com.j4mb.ledger.posting.api;

import com.j4mb.ledger.journal.domain.TransactionType;
import com.j4mb.ledger.posting.domain.PostingRule;
import java.time.Instant;
import java.util.UUID;

public record PostingRuleResponse(
        UUID id,
        String eventType,
        String ruleName,
        TransactionType transactionType,
        String debitCoaCode,
        String creditCoaCode,
        String descriptionTemplate,
        boolean active,
        short priority,
        String conditions,
        String multiLegRules,
        Instant createdAtUtc,
        Instant updatedAtUtc,
        String createdBy,
        String updatedBy
) {
    public static PostingRuleResponse from(PostingRule r) {
        return new PostingRuleResponse(
                r.getId(), r.getEventType(), r.getRuleName(), r.getTransactionType(),
                r.getDebitCoaCode(), r.getCreditCoaCode(), r.getDescriptionTemplate(),
                r.isActive(), r.getPriority(), r.getConditions(), r.getMultiLegRules(),
                r.getCreatedAtUtc(), r.getUpdatedAtUtc(), r.getCreatedBy(), r.getUpdatedBy());
    }
}
