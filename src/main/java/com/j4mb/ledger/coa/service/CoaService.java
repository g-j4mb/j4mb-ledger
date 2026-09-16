package com.j4mb.ledger.coa.service;

import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.domain.NormalBalance;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CoaService {

    private final CoaNodeRepository coaNodeRepository;
    private final AccountRepository  accountRepository;
    private final UserContext        userContext;

    CoaService(CoaNodeRepository coaNodeRepository, AccountRepository accountRepository,
               UserContext userContext) {
        this.coaNodeRepository = coaNodeRepository;
        this.accountRepository  = accountRepository;
        this.userContext        = userContext;
    }

    public List<CoaNode> listRoots() {
        return coaNodeRepository.findByParentIdIsNullOrderByDisplayOrder();
    }

    public List<CoaNode> listChildren(UUID parentId) {
        return coaNodeRepository.findByParentId(parentId);
    }

    public CoaNode findByCode(String code) {
        return coaNodeRepository.findByCode(code)
                .orElseThrow(() -> new LedgerException("COA node not found: " + code));
    }

    public CoaNode findById(UUID id) {
        return coaNodeRepository.findById(id)
                .orElseThrow(() -> new LedgerException("COA node not found: " + id));
    }

    @Transactional
    public CoaNode create(UUID parentId, String code, String name, AccountType type,
                          NormalBalance normalBalance, boolean postable, int displayOrder,
                          Boolean allowNegativeBalance) {
        if (coaNodeRepository.findByCode(code).isPresent()) {
            throw new LedgerException("COA code already exists: " + code);
        }
        String fullPath;
        int depth;
        if (parentId == null) {
            fullPath = code;
            depth    = 0;
        } else {
            CoaNode parent = coaNodeRepository.findById(parentId)
                    .orElseThrow(() -> new LedgerException("Parent COA node not found: " + parentId));
            fullPath = parent.getFullPath() + "." + code;
            depth    = parent.getDepth() + 1;
        }
        CoaNode node = CoaNode.create(parentId, code, name, fullPath, depth, type, normalBalance,
                displayOrder, userContext.getUserId());
        node.markPostable(postable);
        if (allowNegativeBalance != null && !allowNegativeBalance) {
            node.disallowNegativeBalance();
        }
        return coaNodeRepository.save(node);
    }

    @Transactional
    public void freeze(UUID id) {
        findById(id).freeze();
    }

    @Transactional
    public CoaNode update(UUID id, String name, String description, Integer displayOrder,
                          Boolean allowNegativeBalance, String nodeRole, Boolean postable) {
        CoaNode node = findById(id);
        node.updateName(name);
        node.updateDescription(description);
        if (displayOrder != null) node.updateDisplayOrder(displayOrder);
        if (allowNegativeBalance != null) {
            if (allowNegativeBalance) node.allowNegativeBalance();
            else node.disallowNegativeBalance();
        }
        if (nodeRole != null) node.updateNodeRole(nodeRole);
        if (postable != null)  node.markPostable(postable);
        return coaNodeRepository.save(node);
    }

    @Transactional
    public void delete(UUID id) {
        if (coaNodeRepository.existsByParentId(id)) {
            throw new LedgerException("Cannot delete COA node with children: " + id);
        }
        if (accountRepository.existsByCoaNodeId(id)) {
            throw new LedgerException("Cannot delete COA node with linked accounts: " + id);
        }
        coaNodeRepository.deleteById(id);
    }
}
