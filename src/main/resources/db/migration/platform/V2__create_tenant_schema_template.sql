-- =============================================================================
-- V2__create_tenant_schema_template.sql
--
-- Creates the tenant_template schema: the canonical DDL for all tenant schemas.
-- All tables are schema-qualified as tenant_template.*
-- Real tenant schemas are provisioned by running V1-V8 tenant migrations
-- via TenantMigrator (Flyway per-tenant with search_path set).
--
-- No tenant_id columns — schema IS the isolation boundary.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS tenant_template;

-- -----------------------------------------------------------------------------
-- V1: COA Domain — Chart of Accounts
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.coa_nodes (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id       UUID         REFERENCES tenant_template.coa_nodes(id),
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    full_path       VARCHAR(500) NOT NULL,
    depth           INT          NOT NULL DEFAULT 0,
    account_type    VARCHAR(30)  NOT NULL,     -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    normal_balance  VARCHAR(10)  NOT NULL,     -- DEBIT, CREDIT
    is_postable     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_frozen       BOOLEAN      NOT NULL DEFAULT FALSE,
    description     TEXT,
    display_order   INT          NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (code)
);
CREATE INDEX idx_tt_coa_nodes_parent ON tenant_template.coa_nodes(parent_id);
CREATE INDEX idx_tt_coa_nodes_type   ON tenant_template.coa_nodes(account_type);

-- -----------------------------------------------------------------------------
-- V2: Fiscal Domain — fiscal years and periods
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.fsc_fiscal_years (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    year_name       VARCHAR(20) NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    status          SMALLINT    NOT NULL DEFAULT 0,  -- 0=OPEN 1=CLOSED 2=LOCKED
    closed_at_utc   TIMESTAMPTZ,
    closed_by       VARCHAR(255),
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at_utc  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (year_name)
);
CREATE TABLE tenant_template.fsc_periods (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fiscal_year_id  UUID        NOT NULL REFERENCES tenant_template.fsc_fiscal_years(id),
    period_name     VARCHAR(20) NOT NULL,
    period_number   SMALLINT    NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    status          SMALLINT    NOT NULL DEFAULT 0,  -- 0=OPEN 1=CLOSED 2=LOCKED
    closed_at_utc   TIMESTAMPTZ,
    closed_by       VARCHAR(255),
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at_utc  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (fiscal_year_id, period_number)
);
CREATE INDEX idx_tt_fsc_periods_year  ON tenant_template.fsc_periods(fiscal_year_id);
CREATE INDEX idx_tt_fsc_periods_dates ON tenant_template.fsc_periods(start_date, end_date);

-- -----------------------------------------------------------------------------
-- V3: Account Domain — operational accounts linked to COA
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.acc_accounts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    coa_node_id     UUID            NOT NULL REFERENCES tenant_template.coa_nodes(id),
    account_number  VARCHAR(30)     NOT NULL UNIQUE,
    name            VARCHAR(200)    NOT NULL,
    currency_code   CHAR(3)         NOT NULL,
    status          SMALLINT        NOT NULL DEFAULT 0,  -- 0=ACTIVE 1=FROZEN 2=CLOSED
    overdraft_limit NUMERIC(20,4)   NOT NULL DEFAULT 0,
    external_ref    VARCHAR(100),
    description     TEXT,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tt_acc_accounts_coa    ON tenant_template.acc_accounts(coa_node_id);
CREATE INDEX idx_tt_acc_accounts_status ON tenant_template.acc_accounts(status);

-- -----------------------------------------------------------------------------
-- V4: Currency Domain
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.cur_currencies (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    currency_code   CHAR(3)     NOT NULL UNIQUE,
    currency_name   VARCHAR(100) NOT NULL,
    is_base_currency BOOLEAN    NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    decimal_places  INT         NOT NULL DEFAULT 2,
    created_at_utc  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE tenant_template.cur_exchange_rates (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency   CHAR(3)         NOT NULL,
    to_currency     CHAR(3)         NOT NULL,
    rate            NUMERIC(20,8)   NOT NULL CHECK (rate > 0),
    rate_type       SMALLINT        NOT NULL DEFAULT 0,  -- 0=SPOT 1=AVERAGE 2=CLOSING
    effective_date  DATE            NOT NULL,
    source          VARCHAR(50),
    created_at_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (from_currency, to_currency, rate_type, effective_date)
);
CREATE INDEX idx_tt_cur_rates_lookup ON tenant_template.cur_exchange_rates(from_currency, to_currency, effective_date DESC);

-- -----------------------------------------------------------------------------
-- V5: Journal Domain — core double-entry accounting engine
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.jnl_journal_heads (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_number          VARCHAR(30)     NOT NULL UNIQUE,
    transaction_type        SMALLINT        NOT NULL,  -- 1=SALE 2=PURCHASE 3=PAYMENT 4=RECEIPT 5=ADJUSTMENT 6=OPENING 7=CLOSING 8=REVERSAL 9=TRANSFER
    status                  SMALLINT        NOT NULL DEFAULT 0,  -- 0=DRAFT 1=POSTED
    is_reversed             BOOLEAN         NOT NULL DEFAULT FALSE,
    fiscal_period_id        UUID            NOT NULL REFERENCES tenant_template.fsc_periods(id),
    description             TEXT,
    reference               VARCHAR(100),
    source_system           VARCHAR(50),
    source_document_id      VARCHAR(100),
    reversal_of_journal_id  UUID            REFERENCES tenant_template.jnl_journal_heads(id),
    reversal_reason         TEXT,
    posted_by               VARCHAR(255),
    posted_at_utc           TIMESTAMPTZ,
    total_debit             NUMERIC(20,4)   NOT NULL DEFAULT 0,
    total_credit            NUMERIC(20,4)   NOT NULL DEFAULT 0,
    currency_code           CHAR(3)         NOT NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at_utc          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at_utc          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_double_entry CHECK (status != 1 OR total_debit = total_credit)
);
CREATE TABLE tenant_template.jnl_journal_lines (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id          UUID            NOT NULL REFERENCES tenant_template.jnl_journal_heads(id),
    line_number         SMALLINT        NOT NULL,
    account_id          UUID            NOT NULL REFERENCES tenant_template.acc_accounts(id),
    entry_type          SMALLINT        NOT NULL,  -- 0=DEBIT 1=CREDIT
    amount              NUMERIC(20,4)   NOT NULL CHECK (amount > 0),
    currency_code       CHAR(3)         NOT NULL,
    base_currency_code  CHAR(3)         NOT NULL,
    base_amount         NUMERIC(20,4)   NOT NULL,
    exchange_rate       NUMERIC(20,8)   NOT NULL DEFAULT 1,
    description         TEXT,
    cost_center         VARCHAR(50),
    project_code        VARCHAR(50),
    created_at_utc      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (journal_id, line_number)
);
CREATE INDEX idx_tt_jnl_heads_period  ON tenant_template.jnl_journal_heads(fiscal_period_id);
CREATE INDEX idx_tt_jnl_heads_status  ON tenant_template.jnl_journal_heads(status);
CREATE INDEX idx_tt_jnl_heads_source  ON tenant_template.jnl_journal_heads(source_system, source_document_id);
CREATE INDEX idx_tt_jnl_lines_journal ON tenant_template.jnl_journal_lines(journal_id);
CREATE INDEX idx_tt_jnl_lines_account ON tenant_template.jnl_journal_lines(account_id);

-- -----------------------------------------------------------------------------
-- V6: Posting Engine Domain
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.pst_posting_rules (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type           VARCHAR(100) NOT NULL,
    rule_name            VARCHAR(100) NOT NULL,
    transaction_type     SMALLINT     NOT NULL,
    debit_coa_code       VARCHAR(50)  NOT NULL,
    credit_coa_code      VARCHAR(50)  NOT NULL,
    description_template VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    priority             SMALLINT     NOT NULL DEFAULT 0,
    conditions           JSONB,
    multi_leg_rules      JSONB,
    created_at_utc       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at_utc       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (event_type, rule_name)
);
CREATE TABLE tenant_template.pst_idempotency_keys (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source_system     VARCHAR(50)  NOT NULL,
    source_event_id   VARCHAR(200) NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    journal_id        UUID         REFERENCES tenant_template.jnl_journal_heads(id),
    status            SMALLINT     NOT NULL DEFAULT 0,  -- 0=PROCESSED 1=FAILED 2=SKIPPED
    error_message     TEXT,
    processed_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (source_system, source_event_id)
);
CREATE INDEX idx_tt_pst_rules_event     ON tenant_template.pst_posting_rules(event_type, priority DESC) WHERE is_active = TRUE;
CREATE INDEX idx_tt_pst_idempotency_key ON tenant_template.pst_idempotency_keys(source_system, source_event_id);

-- -----------------------------------------------------------------------------
-- V7: Balance Domain — per-period balance cache
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.bal_account_balances (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID            NOT NULL REFERENCES tenant_template.acc_accounts(id),
    fiscal_period_id  UUID            NOT NULL REFERENCES tenant_template.fsc_periods(id),
    currency_code     CHAR(3)         NOT NULL,
    opening_debit     NUMERIC(20,4)   NOT NULL DEFAULT 0,
    opening_credit    NUMERIC(20,4)   NOT NULL DEFAULT 0,
    period_debit      NUMERIC(20,4)   NOT NULL DEFAULT 0,
    period_credit     NUMERIC(20,4)   NOT NULL DEFAULT 0,
    closing_debit     NUMERIC(20,4)   NOT NULL DEFAULT 0,
    closing_credit    NUMERIC(20,4)   NOT NULL DEFAULT 0,
    last_rebuilt_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version           BIGINT          NOT NULL DEFAULT 0,
    UNIQUE (account_id, fiscal_period_id, currency_code)
);
CREATE INDEX idx_tt_bal_balances_account ON tenant_template.bal_account_balances(account_id);
CREATE INDEX idx_tt_bal_balances_period  ON tenant_template.bal_account_balances(fiscal_period_id);

-- -----------------------------------------------------------------------------
-- V8: Audit Domain — INSERT-ONLY; never UPDATE or DELETE
-- -----------------------------------------------------------------------------
CREATE TABLE tenant_template.aud_audit_logs (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type      VARCHAR(50)  NOT NULL,   -- JOURNAL, PERIOD, ACCOUNT, COA_NODE, POSTING_RULE
    entity_id        UUID         NOT NULL,
    action           VARCHAR(50)  NOT NULL,   -- CREATED, POSTED, REVERSED, CLOSED, LOCKED, UPDATED
    performed_by     VARCHAR(255),
    performed_at_utc TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    before_state     JSONB,
    after_state      JSONB,
    ip_address       INET,
    user_agent       TEXT
);
CREATE INDEX idx_tt_aud_logs_entity       ON tenant_template.aud_audit_logs(entity_type, entity_id);
CREATE INDEX idx_tt_aud_logs_performed_at ON tenant_template.aud_audit_logs(performed_at_utc DESC);
