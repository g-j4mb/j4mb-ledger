---
name: database-standards
description: "Load when: designing database schemas, writing JPA entities, creating Flyway migrations, building multi-tenant persistence, writing financial data operations, or implementing idempotency. Covers all persistence concerns for J4MB services."
---

# Database Standards

## Objective

Define enterprise-grade database standards for all J4MB platform services.

Database architecture must prioritize: correctness, consistency, auditability, tenant isolation, maintainability, and operational safety.

---

# Technology Standards

| Component | Choice |
|---|---|
| Database | PostgreSQL 16+ |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway (versioned only) |
| Schema strategy | Schema-per-tenant isolation |
| Money type (Java) | `BigDecimal` |
| Money type (SQL) | `NUMERIC(19,4)` |
| Primary keys | UUID |

Never use FLOAT or DOUBLE for money. Never use `ddl-auto=create` or `ddl-auto=update` in production.

---

# Database Design Philosophy

Design databases from:
1. Business domains and invariants
2. Transactional boundaries
3. Audit requirements
4. Tenant isolation

The database is a business integrity layer — not a technical afterthought.

---

# Table Design Standards

## Naming

```sql
-- Tables: plural snake_case
accounts
journal_entries
payment_transactions
tenant_configurations

-- Columns: snake_case
tenant_id
created_at
account_number
journal_entry_id
```

Avoid: camelCase columns, mixed casing, inconsistent pluralization.

## Primary Keys

```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

Always UUID — never sequential auto-increment for exposed resources.

## Foreign Keys

Explicit with cascade rules:

```sql
account_id UUID NOT NULL REFERENCES accounts(id)
```

Never use implicit relationships.

## Audit Fields (Required on All Business Tables)

```sql
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
created_by VARCHAR(255) NOT NULL,
updated_at TIMESTAMP WITH TIME ZONE,
updated_by VARCHAR(255),
deleted_at TIMESTAMP WITH TIME ZONE  -- optional, soft deletes only
```

## Financial Amount Fields

```sql
amount        NUMERIC(19,4) NOT NULL CHECK(amount >= 0),
currency_code VARCHAR(3)   NOT NULL  -- ISO 4217 e.g. MYR, USD
```

## Indexes

Index: all foreign key columns, frequently-queried fields, unique business identifiers.

```sql
CREATE INDEX idx_accounts_tenant_id ON accounts(tenant_id);
CREATE INDEX idx_journal_entries_account_id ON journal_entries(account_id);
CREATE UNIQUE INDEX idx_accounts_number ON accounts(account_number);
```

## Check Constraints

Use explicit business constraints:

```sql
CHECK (amount >= 0)
CHECK (currency_code ~ '^[A-Z]{3}$')
CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
```

---

# Multi-Tenant Architecture

## Strategy: Schema-Per-Tenant

```
platform.tenants          -- tenant metadata
platform.users            -- platform users
platform.subscriptions

tenant_acme.accounts      -- tenant business data
tenant_acme.transactions
tenant_acme.journal_entries

tenant_nova.accounts
tenant_nova.transactions
```

The shared `platform` schema stores tenant metadata and provisioning state. Each tenant owns its isolated business schema.

## Tenant Context

```java
public class TenantContext {
    private UUID tenantId;
    private String tenantCode;
    private String schemaName;  // e.g. "tenant_acme"
}
```

TenantContext must be: request-scoped, immutable, traceable.

Never use static global tenant state.

## Tenant Resolution Flow

```
JWT
 ↓
Authentication
 ↓
TenantResolver
 ↓
TenantContext
 ↓
PostgreSQL search_path = 'tenant_acme'
```

Never trust raw tenant IDs from request payloads.

## Hibernate Multi-Tenancy Configuration

```java
@Bean
public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
        MultiTenantConnectionProvider connectionProvider,
        CurrentTenantIdentifierResolver tenantResolver) {
    return properties -> {
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
    };
}
```

Schema switching via PostgreSQL `SET search_path TO tenant_acme`. Reset schema state after each connection release.

## Tenant-Aware Cache Keys

```
tenant_acme:account:uuid-123
tenant_nova:account:uuid-456
```

Never share tenant cache state.

---

# JPA Standards

## Entity Design

```java
@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_accounts_number", columnList = "account_number"),
        @Index(name = "idx_accounts_tenant_id", columnList = "tenant_id")
    }
)
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    // Business methods that protect invariants
    public void close() {
        if (this.status == AccountStatus.CLOSED) {
            throw new AccountAlreadyClosedException(this.id);
        }
        this.status = AccountStatus.CLOSED;
        this.updatedAt = Instant.now();
    }
}
```

Entities must encapsulate invariants via business methods. Avoid anemic entities (pure getters/setters with no behavior).

## Fetch Strategy

Default all relationships to `LAZY`. Never use `EAGER` by default.

```java
@OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
private List<Transaction> transactions;
```

Use `@EntityGraph` or `JOIN FETCH` when you need to load associations:

```java
@EntityGraph(attributePaths = {"transactions"})
List<Account> findByTenantId(UUID tenantId);
```

## Optimistic Locking

Use `@Version` on entities that may have concurrent updates:

```java
@Version
private Long version;
```

Critical for: financial entities, concurrent workflow state.

## DTO Separation

Never expose JPA entities in API responses. Always map to response DTOs.

```java
public record AccountResponse(
    UUID id,
    String accountNumber,
    BigDecimal balance,
    String currencyCode,
    Instant createdAt
) {}
```

## N+1 Prevention

Detect with Hibernate statistics in development:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true  # dev only
logging:
  level:
    org.hibernate.stat: DEBUG      # dev only
```

Fix with `JOIN FETCH` or `@EntityGraph`.

---

# Flyway Migration Standards

## Migration Naming

```
V1__init_platform_schema.sql
V2__create_accounts_table.sql
V3__add_tenant_acme_schema.sql
V4__add_journal_entries.sql
```

Requirements: sequential, descriptive, immutable after execution.

**Never edit an executed migration.** Create a new migration instead.

## Immutability

Flyway tracks checksum of each migration. Editing an executed migration causes startup failure. This is intentional — protect the invariant.

## Schema-Per-Tenant Migrations

All tenant schemas must execute identical migration history:

```
tenant_acme → V15  (current)
tenant_nova → V15  (current)
```

Schema drift across tenants is a critical defect.

## Tenant Provisioning

New tenant onboarding:
1. Create tenant schema
2. Execute all Flyway migrations against that schema
3. Register tenant metadata in `platform.tenants`
4. Validate schema consistency

## Destructive Change Strategy

For destructive operations (DROP COLUMN, ALTER TYPE):
1. Deprecate the column (keep it, stop writing to it)
2. Migrate all existing data to new structure
3. Remove in a later migration after confirming no reads

Never drop columns in a single deployment if they are still being read.

## Production Safety

```yaml
spring:
  flyway:
    enabled: true
    validate-on-migrate: true
    out-of-order: false    # strict sequential enforcement
```

Never: modify databases manually in production, bypass migration governance, rely on Hibernate schema generation.

---

# Financial System Standards

## Core Invariants

```
Ledger posting: SUM(debit amounts) == SUM(credit amounts)
Transaction amounts: amount > 0
Currency: all legs of a transaction use the same currency
Financial history: append-only, never mutate
```

These invariants must NEVER break.

## BigDecimal Rules

```java
// ✅ Correct
BigDecimal debit = new BigDecimal("1500.00");
BigDecimal credit = new BigDecimal("1500.00");
debit.compareTo(credit) == 0  // balanced

// ❌ Wrong
double amount = 1500.0;  // precision loss
float amount = 1500f;    // precision loss
```

## Append-Only Ledger

Journal entries are immutable. Financial history must be preserved:

```java
// ✅ Correct — create new correcting entry
JournalEntry correcting = JournalEntry.reversal(originalEntry, reason);
journalEntryRepository.save(correcting);

// ❌ Wrong — mutating history
originalEntry.setAmount(correctedAmount);  // NEVER
journalEntryRepository.save(originalEntry);
```

## External Payment Integration

External payment integrations must support:
- Retry safety (idempotent)
- Reconciliation workflows
- Traceability (every payment linked to internal reference)
- Audit trail (every status transition logged)

---

# Transaction Management

## Transaction Boundary

Transactions belong in the application/service layer, managed by `@Transactional`.

```java
@Service
@Transactional(readOnly = true)  // default for queries
public class AccountService {

    @Transactional  // write operations override
    public AccountResponse create(CreateAccountRequest request) {
        Account account = new Account(request.accountName(), tenantContext.getTenantId());
        account = accountRepository.save(account);
        eventPublisher.publish(new AccountCreated(account.getId()));
        return accountMapper.toResponse(account);
    }
}
```

Never manage transactions in controllers. Never use transaction management in repositories.

## ACID for Financial Operations

Critical financial workflows must preserve:
- **Atomicity**: ledger posting is one atomic operation
- **Consistency**: domain invariants held before and after
- **Isolation**: concurrent transactions don't corrupt each other
- **Durability**: committed transactions survive failures

## Distributed Workflows

Avoid distributed ACID assumptions. Prefer:
- Outbox pattern for reliable Kafka publishing
- Eventual consistency with compensating transactions
- Saga orchestration for multi-service workflows

---

# Idempotency Standards

## Core Principle

Same request + same idempotency key → same result, always.

Critical for: payment processing, ledger posting, Kafka consumers, external callbacks.

## API Idempotency

```http
POST /api/v1/payments
Idempotency-Key: payment-ref-12345-retry-1
Content-Type: application/json
```

## Database Deduplication

```sql
-- Unique constraint on business reference
CREATE UNIQUE INDEX idx_transactions_reference
ON transactions(idempotency_key, tenant_id);
```

## Kafka Idempotency

Assume at-least-once delivery. Build consumers to be replay-safe:
1. Check if `eventId` already processed
2. If yes: return success without re-executing
3. If no: execute, then record as processed

Track processed events:

```sql
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

---

# Auditability Standards

All financial, security, and tenant-critical operations must record:
- `created_at`, `created_by`, `updated_at`, `updated_by`
- `tenantId`, `traceId`, `correlationId`
- Before/after state for changes to critical business data

Security events to audit: login attempts, permission denials, role changes, token failures.

Never log: passwords, secrets, or PII in audit trails.

Audit trails must be: tenant-isolated, immutable, searchable by traceId.

---

# Query Standards

```java
// ✅ Avoid N+1
@Query("SELECT a FROM Account a JOIN FETCH a.transactions WHERE a.tenantId = :tenantId")
List<Account> findWithTransactions(@Param("tenantId") UUID tenantId);

// ✅ Use projections for list endpoints
public record AccountSummary(UUID id, String accountNumber, BigDecimal balance) {}

@Query("SELECT new com.j4mb.ledger.account.AccountSummary(a.id, a.accountNumber, a.balance) FROM Account a WHERE a.tenantId = :tenantId")
List<AccountSummary> findSummaries(@Param("tenantId") UUID tenantId);

// ✅ Paginate large result sets
Page<AccountSummary> findByTenantId(UUID tenantId, Pageable pageable);

// ❌ Never do this in production
List<Account> findAll();  // unbounded
```

---

# AI Code Generation Rules

AI-generated database code must:
- Use UUID primary keys
- Use `NUMERIC(19,4)` for money (never FLOAT/DOUBLE)
- Include standard audit fields on all business tables
- Support schema-per-tenant architecture
- Use explicit Flyway migrations (never `ddl-auto`)
- Use `@Version` for optimistic locking on financial entities
- Separate JPA entities from response DTOs
- Default all JPA relationships to LAZY
- Support idempotency for all financial operations
- Preserve append-only financial history

Do not generate:
- Float or double money fields
- `ddl-auto=create` or `ddl-auto=update`
- Entity exposure in API responses
- Hidden lazy-loading N+1 problems
- Uncontrolled cascades
- Tenant-unsafe queries that mix tenant data
- Mutable financial history
