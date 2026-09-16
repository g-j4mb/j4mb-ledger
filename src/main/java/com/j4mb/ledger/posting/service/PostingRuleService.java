package com.j4mb.ledger.posting.service;

import com.j4mb.ledger.journal.domain.TransactionType;
import com.j4mb.ledger.posting.domain.PostingRule;
import com.j4mb.ledger.posting.repository.PostingRuleRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PostingRuleService {

    private final PostingRuleRepository postingRuleRepository;
    private final UserContext           userContext;

    PostingRuleService(PostingRuleRepository postingRuleRepository, UserContext userContext) {
        this.postingRuleRepository = postingRuleRepository;
        this.userContext           = userContext;
    }

    public PostingRule findById(UUID id) {
        return postingRuleRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Posting rule not found: " + id));
    }

    public Page<PostingRule> listActive(Pageable pageable) {
        return postingRuleRepository.findByActiveTrue(pageable);
    }

    public Page<PostingRule> listAll(Pageable pageable) {
        return postingRuleRepository.findAll(pageable);
    }

    @Transactional
    public PostingRule create(String eventType, String ruleName, TransactionType transactionType,
                              String debitCoaCode, String creditCoaCode, String descriptionTemplate,
                              short priority, String conditions, String multiLegRules) {
        PostingRule rule = PostingRule.create(eventType, ruleName, transactionType,
                debitCoaCode, creditCoaCode, descriptionTemplate, priority, userContext.getUserId());
        if (conditions != null)    rule.updateConditions(conditions);
        if (multiLegRules != null) rule.updateMultiLegRules(multiLegRules);
        return postingRuleRepository.save(rule);
    }

    @Transactional
    public PostingRule update(UUID id, String debitCoaCode, String creditCoaCode,
                              String descriptionTemplate, Boolean active, Integer priority,
                              String conditions, String multiLegRules) {
        PostingRule rule = findById(id);
        if (debitCoaCode != null && !debitCoaCode.isBlank())    rule.updateDebitCoaCode(debitCoaCode);
        if (creditCoaCode != null && !creditCoaCode.isBlank())  rule.updateCreditCoaCode(creditCoaCode);
        if (descriptionTemplate != null)                        rule.updateDescriptionTemplate(descriptionTemplate);
        if (active != null)                                     rule.updateActive(active);
        if (priority != null)                                   rule.updatePriority(priority.shortValue());
        if (conditions != null)                                 rule.updateConditions(conditions);
        if (multiLegRules != null)                              rule.updateMultiLegRules(multiLegRules);
        rule.updateUpdatedBy(userContext.getUserId());
        return postingRuleRepository.save(rule);
    }

    @Transactional
    public PostingRule deactivate(UUID id) {
        PostingRule rule = findById(id);
        rule.updateActive(false);
        rule.updateUpdatedBy(userContext.getUserId());
        return postingRuleRepository.save(rule);
    }
}
