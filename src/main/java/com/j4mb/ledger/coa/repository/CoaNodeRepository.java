package com.j4mb.ledger.coa.repository;

import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.CoaNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoaNodeRepository extends JpaRepository<CoaNode, UUID> {
    Optional<CoaNode> findByCode(String code);
    List<CoaNode> findByParentId(UUID parentId);
    boolean existsByParentId(UUID parentId);
    List<CoaNode> findByAccountType(AccountType accountType);
    List<CoaNode> findByParentIdIsNullOrderByDisplayOrder(); // root nodes
}
