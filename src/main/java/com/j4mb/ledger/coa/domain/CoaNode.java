package com.j4mb.ledger.coa.domain;

import com.j4mb.ledger.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "coa_nodes")
public class CoaNode extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "full_path", nullable = false, length = 500)
    private String fullPath;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 10)
    private NormalBalance normalBalance;

    @Column(name = "is_postable", nullable = false)
    private boolean postable;

    @Column(name = "is_frozen", nullable = false)
    private boolean frozen;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "allow_negative_balance", nullable = false)
    private boolean allowNegativeBalance = true;

    @Column(name = "node_role", length = 30)
    private String nodeRole;

    protected CoaNode() {}

    public static CoaNode create(UUID parentId, String code, String name, String fullPath,
                                  int depth, AccountType accountType, NormalBalance normalBalance,
                                  int displayOrder, String createdBy) {
        CoaNode node = new CoaNode();
        node.id            = UUID.randomUUID();
        node.parentId      = parentId;
        node.code          = code;
        node.name          = name;
        node.fullPath      = fullPath;
        node.depth         = depth;
        node.accountType   = accountType;
        node.normalBalance = normalBalance;
        node.postable      = parentId != null; // leaf nodes default postable; root nodes are not
        node.frozen        = false;
        node.displayOrder  = displayOrder;
        node.setCreatedBy(createdBy);
        node.setUpdatedBy(createdBy);
        return node;
    }

    public void freeze() { this.frozen = true; }
    public void unfreeze() { this.frozen = false; }
    public void markPostable(boolean postable) { this.postable = postable; }
    public void disallowNegativeBalance() { this.allowNegativeBalance = false; }
    public void allowNegativeBalance()    { this.allowNegativeBalance = true; }
    public void setNodeRole(String role)  { this.nodeRole = role; }

    public void updateName(String name)                { if (name != null) this.name = name; }
    public void updateDescription(String description)  { this.description = description; }
    public void updateDisplayOrder(int displayOrder)   { this.displayOrder = displayOrder; }
    public void updateNodeRole(String nodeRole)        { this.nodeRole = nodeRole; }

    public UUID         getId()           { return id; }
    public UUID         getParentId()     { return parentId; }
    public String       getCode()         { return code; }
    public String       getName()         { return name; }
    public String       getFullPath()     { return fullPath; }
    public int          getDepth()        { return depth; }
    public AccountType  getAccountType()  { return accountType; }
    public NormalBalance getNormalBalance() { return normalBalance; }
    public boolean      isPostable()              { return postable; }
    public boolean      isFrozen()                { return frozen; }
    public String       getDescription()          { return description; }
    public int          getDisplayOrder()         { return displayOrder; }
    public boolean      isAllowNegativeBalance()  { return allowNegativeBalance; }
    public String       getNodeRole()             { return nodeRole; }
}
