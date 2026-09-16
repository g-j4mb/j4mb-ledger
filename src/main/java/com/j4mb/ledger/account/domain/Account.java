package com.j4mb.ledger.account.domain;

import com.j4mb.ledger.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "acc_accounts")
public class Account extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "coa_node_id", nullable = false)
    private UUID coaNodeId;

    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "overdraft_limit", nullable = false, precision = 20, scale = 4)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column(name = "external_ref", length = 100)
    private String externalRef;

    @Column(name = "description")
    private String description;

    protected Account() {}

    public static Account create(UUID coaNodeId, String accountNumber, String name,
                                  String currencyCode, String createdBy) {
        Account a        = new Account();
        a.id             = UUID.randomUUID();
        a.coaNodeId      = coaNodeId;
        a.accountNumber  = accountNumber;
        a.name           = name;
        a.currencyCode   = currencyCode;
        a.status         = AccountStatus.ACTIVE;
        a.overdraftLimit = BigDecimal.ZERO;
        a.setCreatedBy(createdBy);
        a.setUpdatedBy(createdBy);
        return a;
    }

    public void updateName(String name)             { if (name != null) this.name = name; }
    public void updateDescription(String description) { this.description = description; }
    public void updateOverdraftLimit(java.math.BigDecimal limit) { this.overdraftLimit = limit; }
    public void updateExternalRef(String ref)        { this.externalRef = ref; }

    public void freeze() {
        if (this.status != AccountStatus.ACTIVE) throw new IllegalStateException("Only ACTIVE accounts can be frozen");
        this.status = AccountStatus.FROZEN;
    }

    public void close() {
        if (this.status == AccountStatus.CLOSED) throw new IllegalStateException("Account is already CLOSED");
        this.status = AccountStatus.CLOSED;
    }

    public void activate() {
        if (this.status != AccountStatus.FROZEN) throw new IllegalStateException("Only FROZEN accounts can be activated");
        this.status = AccountStatus.ACTIVE;
    }

    public boolean isActive() { return status == AccountStatus.ACTIVE; }
    public boolean isFrozen() { return status == AccountStatus.FROZEN; }
    public boolean isClosed() { return status == AccountStatus.CLOSED; }

    public UUID          getId()             { return id; }
    public UUID          getCoaNodeId()      { return coaNodeId; }
    public String        getAccountNumber()  { return accountNumber; }
    public String        getName()           { return name; }
    public String        getCurrencyCode()   { return currencyCode; }
    public AccountStatus getStatus()         { return status; }
    public BigDecimal    getOverdraftLimit() { return overdraftLimit; }
    public String        getExternalRef()    { return externalRef; }
    public String        getDescription()    { return description; }
}
