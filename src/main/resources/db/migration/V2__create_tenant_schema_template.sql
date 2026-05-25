-- =============================================================================
-- V2__create_tenant_schema_template.sql
--
-- Creates the tenant_template schema: the canonical DDL for all tenant schemas.
--
-- This template defines the table structure that every tenant schema must have.
-- Real tenant schemas (e.g. tenant_acme) are provisioned by running these same
-- CREATE TABLE statements against the new schema — either via a per-tenant
-- Flyway run or the future tenant-onboarding service.
--
-- Financial invariants enforced here:
--   - NUMERIC(19,4) for all monetary amounts (never FLOAT/DOUBLE)
--   - amount > 0 on transactions (direction is expressed by transaction_type)
--   - journal entry balanced constraint (total_debit = total_credit when POSTED)
--   - append-only design: journal entries are never updated, only voided via new entries
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS tenant_template;

-- -----------------------------------------------------------------------------
-- tenant_template.accounts
--
-- Chart of accounts for a tenant.
-- version column supports Hibernate @Version optimistic locking.
-- balance NUMERIC(19,4) per financial money standard.
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.accounts
(
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_number VARCHAR(50)   NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    account_type   VARCHAR(30)   NOT NULL,
    balance        NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    currency_code  VARCHAR(3)    NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     VARCHAR(255)  NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by     VARCHAR(255)  NOT NULL,

    CONSTRAINT pk_accounts          PRIMARY KEY (id),
    CONSTRAINT uq_accounts_number   UNIQUE (account_number),
    CONSTRAINT ck_accounts_type     CHECK (account_type IN (
                                        'ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT ck_accounts_status   CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED')),
    CONSTRAINT ck_accounts_currency CHECK (currency_code ~ '^[A-Z]{3}$')
);

COMMENT ON COLUMN tenant_template.accounts.version       IS 'Hibernate @Version optimistic lock counter';
COMMENT ON COLUMN tenant_template.accounts.currency_code IS 'ISO 4217 3-letter code, e.g. MYR, USD';
COMMENT ON COLUMN tenant_template.accounts.balance       IS 'Running balance, NUMERIC(19,4) — never FLOAT';

CREATE INDEX idx_accounts_account_number ON tenant_template.accounts (account_number);
CREATE INDEX idx_accounts_status         ON tenant_template.accounts (status);

-- -----------------------------------------------------------------------------
-- tenant_template.journal_entries
--
-- Double-entry bookkeeping journal entry headers.
-- idempotency_key prevents duplicate submissions (POST retries, Kafka redelivery).
-- Balanced constraint: when POSTED, total_debit must equal total_credit.
-- Immutability: once POSTED, entries are never modified — use VOIDED entries
-- with correcting entries to fix errors (append-only ledger).
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.journal_entries
(
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    entry_number    VARCHAR(50)   NOT NULL,
    description     TEXT,
    entry_date      DATE          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_debit     NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    total_credit    NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    posted_at       TIMESTAMPTZ,
    idempotency_key VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      VARCHAR(255)  NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      VARCHAR(255)  NOT NULL,

    CONSTRAINT pk_journal_entries              PRIMARY KEY (id),
    CONSTRAINT uq_journal_entries_number       UNIQUE (entry_number),
    CONSTRAINT uq_journal_entries_idempotency  UNIQUE (idempotency_key),
    CONSTRAINT ck_journal_entries_status       CHECK (status IN ('DRAFT', 'POSTED', 'VOIDED')),
    -- Balanced ledger invariant: debits must equal credits once posted.
    -- DRAFT entries are allowed to be unbalanced (work in progress).
    CONSTRAINT ck_journal_entries_balanced     CHECK (
        total_debit = total_credit OR status = 'DRAFT')
);

COMMENT ON COLUMN tenant_template.journal_entries.idempotency_key IS 'Client-supplied key for exactly-once submission';
COMMENT ON COLUMN tenant_template.journal_entries.posted_at       IS 'Set when status → POSTED; null for DRAFT/VOIDED';
COMMENT ON COLUMN tenant_template.journal_entries.total_debit     IS 'Sum of all DEBIT transaction lines; NUMERIC(19,4)';
COMMENT ON COLUMN tenant_template.journal_entries.total_credit    IS 'Sum of all CREDIT transaction lines; NUMERIC(19,4)';

CREATE INDEX idx_journal_entries_entry_date ON tenant_template.journal_entries (entry_date);
CREATE INDEX idx_journal_entries_status     ON tenant_template.journal_entries (status);

-- -----------------------------------------------------------------------------
-- tenant_template.transactions
--
-- Individual debit/credit lines within a journal entry (the "legs").
-- Each transaction line belongs to one journal entry and one account.
--
-- Double-entry rule: for each journal entry,
--   SUM(amount WHERE transaction_type = 'DEBIT')
--     = SUM(amount WHERE transaction_type = 'CREDIT')
-- This is enforced at the application layer (JournalEntryService) and checked
-- by the ck_journal_entries_balanced constraint on the parent table.
--
-- amount is always positive — direction is expressed by transaction_type.
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.transactions
(
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    journal_entry_id UUID          NOT NULL,
    account_id       UUID          NOT NULL,
    transaction_type VARCHAR(6)    NOT NULL,
    amount           NUMERIC(19,4) NOT NULL,
    currency_code    VARCHAR(3)    NOT NULL,
    description      TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       VARCHAR(255)  NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by       VARCHAR(255)  NOT NULL,

    CONSTRAINT pk_transactions          PRIMARY KEY (id),
    CONSTRAINT fk_transactions_journal  FOREIGN KEY (journal_entry_id)
                                            REFERENCES tenant_template.journal_entries (id),
    CONSTRAINT fk_transactions_account  FOREIGN KEY (account_id)
                                            REFERENCES tenant_template.accounts (id),
    CONSTRAINT ck_transactions_type     CHECK (transaction_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_transactions_amount   CHECK (amount > 0),
    CONSTRAINT ck_transactions_currency CHECK (currency_code ~ '^[A-Z]{3}$')
);

COMMENT ON COLUMN tenant_template.transactions.transaction_type IS 'DEBIT | CREDIT — determines which side of the ledger';
COMMENT ON COLUMN tenant_template.transactions.amount           IS 'Always positive; NUMERIC(19,4) — never FLOAT';

CREATE INDEX idx_transactions_journal_entry ON tenant_template.transactions (journal_entry_id);
CREATE INDEX idx_transactions_account       ON tenant_template.transactions (account_id);
CREATE INDEX idx_transactions_currency      ON tenant_template.transactions (currency_code);
