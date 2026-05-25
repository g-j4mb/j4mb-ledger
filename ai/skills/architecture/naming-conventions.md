---
name: naming-conventions
description: "Load when: naming any class, method, variable, table, Kafka topic, REST endpoint, Docker container, or environment variable. Names become architectural contracts — establish them correctly from the start."
---

# Naming Conventions

## Objective

Define consistent naming conventions across: backend services, APIs, databases, events, infrastructure, and AI-generated code.

Naming consistency improves readability, maintainability, discoverability, and operational clarity.

---

# General Naming Principles

Names must:
- Reveal intent and use domain language
- Remain explicit, stable, and consistent

Prefer: business terminology, descriptive naming, predictable patterns.

Avoid: abbreviations, ambiguous names, technical jargon leakage, inconsistent pluralization.

---

# Java Naming

## Packages

Use lowercase, domain-oriented structure.

```
com.j4mb.ledger.account
com.j4mb.payment.transaction
```

Avoid deep unnecessary nesting and generic technical packaging (`service.impl.utils.common`).

## Classes — PascalCase

```java
AccountService        // ✅
PaymentController     // ✅
JournalEntryRepository // ✅

Utils / Manager / Helper / ProcessorFactoryImpl  // ❌
```

## Interfaces — capability-based names

```java
PaymentProcessor    // ✅
NotificationSender  // ✅
TenantResolver      // ✅

IService / IManager  // ❌
```

## Methods — describe behavior clearly

```java
createAccount()        // ✅
postJournalEntry()     // ✅
calculateBalance()     // ✅

process() / handle() / executeStuff()  // ❌
```

## Variables — explain purpose with domain language

```java
tenantId              // ✅
accountBalance        // ✅
transactionTimestamp  // ✅

data / value / obj / tmp  // ❌
```

## Constants — UPPER_SNAKE_CASE

```java
MAX_RETRY_COUNT
DEFAULT_PAGE_SIZE
TOKEN_EXPIRATION_MINUTES
```

Never use magic numbers or hardcoded strings.

---

# DTO Naming

| Type | Pattern | Example |
|---|---|---|
| Request | `Create{Entity}Request` | `CreateAccountRequest` |
| Request | `Update{Entity}Request` | `UpdatePaymentRequest` |
| Response | `{Entity}Response` | `AccountResponse` |
| Response | `{Entity}DetailsResponse` | `PaymentDetailsResponse` |

Avoid: `AccountDTO`, `PaymentData`, `GenericRequest`.

---

# Entity Naming

Use singular nouns:
- `Account` ✅ (not `Accounts`, not `AccountEntity`)
- `Transaction` ✅
- `JournalEntry` ✅

---

# Service / Repository / Exception Naming

```java
// Services
AccountService / PaymentService / NotificationService

// Repositories
AccountRepository / PaymentRepository  (never GenericRepository)

// Exceptions — describe business failures
AccountNotFoundException
InsufficientBalanceException
TenantAccessDeniedException
```

Avoid: `GenericBusinessException`, `ValidationFailedException`.

---

# REST API Naming

## Resource paths — plural, lowercase, versioned

```
/api/v1/accounts           ✅
/api/v1/payments           ✅
/api/v1/journal-entries    ✅

/getAccounts               ❌  (no verbs)
/createPayment             ❌  (no verbs)
```

## Path variables — explicit resource identifiers

```
/accounts/{accountId}
/tenants/{tenantId}/users
```

---

# Event Naming

Events represent completed business facts — past-tense nouns:

```java
AccountCreated      ✅
PaymentProcessed    ✅
InvoiceGenerated    ✅
TenantProvisioned   ✅

CreateAccountEvent  ❌  (command, not event)
PaymentEvent        ❌  (too generic)
```

---

# Kafka Topic Naming

Pattern: `domain.entity.action.v1`

```
ledger.account.created.v1
payment.transaction.completed.v1
notification.email.sent.v1
```

Requirements: lowercase, dot-separated, versioned. Never unversioned topics.

---

# Database Naming

## Tables — plural snake_case

```sql
accounts
journal_entries
payment_transactions
```

## Columns — snake_case

```sql
tenant_id
created_at
account_number
```

Avoid camelCase columns.

## Primary keys

`id UUID PRIMARY KEY`

## Foreign keys

`account_id`, `tenant_id`, `payment_id`

## Audit fields (standard across all business tables)

```sql
created_at TIMESTAMP NOT NULL
created_by VARCHAR(255)
updated_at TIMESTAMP
updated_by VARCHAR(255)
deleted_at TIMESTAMP  -- optional, soft deletes only
```

## Flyway migrations — sequential and descriptive

```
V1__init.sql
V2__create_accounts_table.sql
V3__add_tenant_support.sql
```

Immutable after execution. Never rename or edit executed migrations.

---

# Infrastructure Naming

## Docker

```
j4mb-postgres        (container)
j4mb-ledger-service  (container)
j4mb-network         (network)
```

## Kubernetes

Use lowercase, hyphen-separated:
```
ledger-service
payment-service
```

Avoid camelCase and underscores.

## Environment variables — UPPER_SNAKE_CASE

```
DB_HOST
JWT_SECRET
KAFKA_BOOTSTRAP_SERVERS
SPRING_BOOT_PORT
POSTGRES_PORT
VITE_PORT
```

---

# Repository Naming

Use lowercase hyphen-separated, domain-oriented names:

```
j4mb-ledger
j4mb-payment
j4mb-document-service
j4mb-architecture
```

Avoid: `backend-final`, `test-service`, `microservice-demo`.

---

# Long-Term Naming Stability

Names become architectural contracts.

Changing names later impacts: APIs, databases, integrations, events, observability, documentation.

Prefer stable, intentional, domain-consistent terminology from the beginning.

---

# AI Code Generation Rules

AI-generated names must:
- Follow domain language and remain explicit
- Avoid abbreviations and maintain consistency

AI must never generate:
- Meaningless names (`data`, `value`, `tmp`)
- Temporary naming (`temp`, `test1`)
- Generic utility naming (`Utils`, `Helper`, `Manager`)
- Unclear abstractions
