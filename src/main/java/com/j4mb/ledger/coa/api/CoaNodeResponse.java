package com.j4mb.ledger.coa.api;

import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.domain.NormalBalance;
import java.util.UUID;

public record CoaNodeResponse(
        UUID id, UUID parentId, String code, String name, String fullPath,
        int depth, AccountType accountType, NormalBalance normalBalance,
        boolean postable, boolean frozen, int displayOrder,
        boolean allowNegativeBalance, String nodeRole
) {
    public static CoaNodeResponse from(CoaNode n) {
        return new CoaNodeResponse(n.getId(), n.getParentId(), n.getCode(), n.getName(),
                n.getFullPath(), n.getDepth(), n.getAccountType(), n.getNormalBalance(),
                n.isPostable(), n.isFrozen(), n.getDisplayOrder(),
                n.isAllowNegativeBalance(), n.getNodeRole());
    }
}
