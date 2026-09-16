-- =============================================================================
-- V3__rebuild_tenant_template.sql
--
-- Drops and rebuilds the tenant_template schema to match the current
-- V1–V8 tenant migrations (VARCHAR enums + audit fields).
-- tenant_template is documentation-only: it mirrors what every new tenant
-- schema looks like after full provisioning. It is never used for business data.
-- =============================================================================

DROP SCHEMA IF EXISTS tenant_template CASCADE;
CREATE SCHEMA tenant_template;

-- V1 — COA
CREATE TABLE tenant_template.coa_nodes (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id       UUID         REFERENCES tenant_template.coa_nodes(id),
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    full_path       VARCHAR(500) NOT NULL,
    depth           INT          NOT NULL DEFAULT 0,
    account_type    VARCHAR(30)  NOT NULL CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    normal_balance  VARCHAR(10)  NOT NULL CHECK (normal_balance IN ('DEBIT','CREDIT')),
    is_postable     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_frozen       BOOLEAN      NOT NULL DEFAULT FALSE,
    description     TEXT,
    display_order   INT          NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (code)
);

-- V2 — Fiscal
CREATE TABLE tenant_template.fsc_fiscal_years (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    year_name       VARCHAR(20)  NOT NULL UNIQUE,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED','LOCKED')),
    closed_at_utc   TIMESTAMPTZ,
    closed_by       VARCHAR(255),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE TABLE tenant_template.fsc_periods (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    fiscal_year_id  UUID         NOT NULL REFERENCES tenant_template.fsc_fiscal_years(id),
    period_name     VARCHAR(20)  NOT NULL,
    period_number   SMALLINT     NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED','LOCKED')),
    closed_at_utc   TIMESTAMPTZ,
    closed_by       VARCHAR(255),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (fiscal_year_id, period_number)
);

-- V3 — Accounts
CREATE TABLE tenant_template.acc_accounts (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    coa_node_id     UUID          NOT NULL REFERENCES tenant_template.coa_nodes(id),
    account_number  VARCHAR(30)   NOT NULL UNIQUE,
    name            VARCHAR(200)  NOT NULL,
    currency_code   CHAR(3)       NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    overdraft_limit NUMERIC(20,4) NOT NULL DEFAULT 0,
    external_ref    VARCHAR(100),
    description     TEXT,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_by      VARCHAR(255)  NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(255)  NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at_utc  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- V4 — Currency
CREATE TABLE tenant_template.cur_currencies (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    currency_code    CHAR(3)      NOT NULL UNIQUE,
    currency_name    VARCHAR(100) NOT NULL,
    is_base_currency BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    decimal_places   INT          NOT NULL DEFAULT 2,
    created_by       VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE TABLE tenant_template.cur_exchange_rates (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency   CHAR(3)        NOT NULL,
    to_currency     CHAR(3)        NOT NULL,
    rate            NUMERIC(20,8)  NOT NULL CHECK (rate > 0),
    rate_type       VARCHAR(20)    NOT NULL DEFAULT 'SPOT' CHECK (rate_type IN ('SPOT','AVERAGE','CLOSING')),
    effective_date  DATE           NOT NULL,
    source          VARCHAR(50),
    created_by      VARCHAR(255)   NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (from_currency, to_currency, rate_type, effective_date)
);

-- V5 — Journals
CREATE TABLE tenant_template.jnl_journal_heads (
    id                      UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_number          VARCHAR(30)    NOT NULL UNIQUE,
    transaction_type        VARCHAR(30)    NOT NULL CHECK (transaction_type IN ('SALE','PURCHASE','PAYMENT','RECEIPT','ADJUSTMENT','OPENING','CLOSING','REVERSAL','TRANSFER')),
    status                  VARCHAR(20)    NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','POSTED','CANCELLED')),
    is_reversed             BOOLEAN        NOT NULL DEFAULT FALSE,
    fiscal_period_id        UUID           NOT NULL REFERENCES tenant_template.fsc_periods(id),
    description             TEXT,
    reference               VARCHAR(100),
    source_system           VARCHAR(50),
    source_document_id      VARCHAR(100),
    reversal_of_journal_id  UUID           REFERENCES tenant_template.jnl_journal_heads(id),
    reversal_reason         TEXT,
    posted_by               VARCHAR(255),
    posted_at_utc           TIMESTAMPTZ,
    total_debit             NUMERIC(20,4)  NOT NULL DEFAULT 0,
    total_credit            NUMERIC(20,4)  NOT NULL DEFAULT 0,
    currency_code           CHAR(3)        NOT NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,
    created_by              VARCHAR(255)   NOT NULL DEFAULT 'system',
    updated_by              VARCHAR(255)   NOT NULL DEFAULT 'system',
    created_at_utc          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at_utc          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_double_entry CHECK (status != 'POSTED' OR total_debit = total_credit)
);
CREATE TABLE tenant_template.jnl_journal_lines (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id          UUID           NOT NULL REFERENCES tenant_template.jnl_journal_heads(id),
    line_number         SMALLINT       NOT NULL,
    account_id          UUID           NOT NULL REFERENCES tenant_template.acc_accounts(id),
    entry_type          VARCHAR(10)    NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount              NUMERIC(20,4)  NOT NULL CHECK (amount > 0),
    currency_code       CHAR(3)        NOT NULL,
    base_currency_code  CHAR(3)        NOT NULL,
    base_amount         NUMERIC(20,4)  NOT NULL,
    exchange_rate       NUMERIC(20,8)  NOT NULL DEFAULT 1,
    description         TEXT,
    cost_center         VARCHAR(50),
    project_code        VARCHAR(50),
    created_by          VARCHAR(255)   NOT NULL DEFAULT 'system',
    created_at_utc      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (journal_id, line_number)
);

-- V6 — Posting Engine
CREATE TABLE tenant_template.pst_posting_rules (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type           VARCHAR(100) NOT NULL,
    rule_name            VARCHAR(100) NOT NULL,
    transaction_type     VARCHAR(30)  NOT NULL CHECK (transaction_type IN ('SALE','PURCHASE','PAYMENT','RECEIPT','ADJUSTMENT','OPENING','CLOSING','REVERSAL','TRANSFER')),
    debit_coa_code       VARCHAR(50)  NOT NULL,
    credit_coa_code      VARCHAR(50)  NOT NULL,
    description_template VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    priority             SMALLINT     NOT NULL DEFAULT 0,
    conditions           JSONB,
    multi_leg_rules      JSONB,
    created_by           VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by           VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at_utc       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (event_type, rule_name)
);
CREATE TABLE tenant_template.pst_idempotency_keys (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_system     VARCHAR(50)  NOT NULL,
    source_event_id   VARCHAR(200) NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    journal_id        UUID         REFERENCES tenant_template.jnl_journal_heads(id),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PROCESSED' CHECK (status IN ('PROCESSED','FAILED','SKIPPED')),
    error_message     TEXT,
    created_by        VARCHAR(255) NOT NULL DEFAULT 'system',
    processed_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (source_system, source_event_id)
);

-- V7 — Balances
CREATE TABLE tenant_template.bal_account_balances (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID           NOT NULL REFERENCES tenant_template.acc_accounts(id),
    fiscal_period_id  UUID           NOT NULL REFERENCES tenant_template.fsc_periods(id),
    currency_code     CHAR(3)        NOT NULL,
    opening_debit     NUMERIC(20,4)  NOT NULL DEFAULT 0,
    opening_credit    NUMERIC(20,4)  NOT NULL DEFAULT 0,
    period_debit      NUMERIC(20,4)  NOT NULL DEFAULT 0,
    period_credit     NUMERIC(20,4)  NOT NULL DEFAULT 0,
    closing_debit     NUMERIC(20,4)  NOT NULL DEFAULT 0,
    closing_credit    NUMERIC(20,4)  NOT NULL DEFAULT 0,
    last_rebuilt_utc  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255)   NOT NULL DEFAULT 'system',
    updated_by        VARCHAR(255)   NOT NULL DEFAULT 'system',
    version           BIGINT         NOT NULL DEFAULT 0,
    UNIQUE (account_id, fiscal_period_id, currency_code)
);

-- V8 — Audit
CREATE TABLE tenant_template.aud_audit_logs (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type      VARCHAR(50)  NOT NULL,
    entity_id        UUID         NOT NULL,
    action           VARCHAR(50)  NOT NULL,
    performed_by     VARCHAR(255),
    performed_at_utc TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    before_state     JSONB,
    after_state      JSONB,
    ip_address       INET,
    user_agent       TEXT
);
