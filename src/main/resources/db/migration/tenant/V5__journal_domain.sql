-- Journal Domain — core double-entry accounting engine
CREATE TABLE jnl_journal_heads (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_number          VARCHAR(30)     NOT NULL UNIQUE,
    transaction_type        VARCHAR(30)     NOT NULL CHECK (transaction_type IN ('SALE','PURCHASE','PAYMENT','RECEIPT','ADJUSTMENT','OPENING','CLOSING','REVERSAL','TRANSFER')),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','POSTED','CANCELLED')),
    is_reversed             BOOLEAN         NOT NULL DEFAULT FALSE,
    fiscal_period_id        UUID            NOT NULL REFERENCES fsc_periods(id),
    description             TEXT,
    reference               VARCHAR(100),
    source_system           VARCHAR(50),
    source_document_id      VARCHAR(100),
    reversal_of_journal_id  UUID            REFERENCES jnl_journal_heads(id),
    reversal_reason         TEXT,
    posted_by               VARCHAR(255),
    posted_at_utc           TIMESTAMPTZ,
    total_debit             NUMERIC(20,4)   NOT NULL DEFAULT 0,
    total_credit            NUMERIC(20,4)   NOT NULL DEFAULT 0,
    currency_code           CHAR(3)         NOT NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_by              VARCHAR(255)    NOT NULL DEFAULT 'system',
    updated_by              VARCHAR(255)    NOT NULL DEFAULT 'system',
    created_at_utc          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at_utc          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_double_entry CHECK (status != 'POSTED' OR total_debit = total_credit)
);
CREATE TABLE jnl_journal_lines (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id          UUID            NOT NULL REFERENCES jnl_journal_heads(id),
    line_number         SMALLINT        NOT NULL,
    account_id          UUID            NOT NULL REFERENCES acc_accounts(id),
    entry_type          VARCHAR(10)     NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount              NUMERIC(20,4)   NOT NULL CHECK (amount > 0),
    currency_code       CHAR(3)         NOT NULL,
    base_currency_code  CHAR(3)         NOT NULL,
    base_amount         NUMERIC(20,4)   NOT NULL,
    exchange_rate       NUMERIC(20,8)   NOT NULL DEFAULT 1,
    description         TEXT,
    cost_center         VARCHAR(50),
    project_code        VARCHAR(50),
    created_by          VARCHAR(255)    NOT NULL DEFAULT 'system',
    created_at_utc      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (journal_id, line_number)
);
CREATE INDEX idx_jnl_heads_period  ON jnl_journal_heads(fiscal_period_id);
CREATE INDEX idx_jnl_heads_status  ON jnl_journal_heads(status);
CREATE INDEX idx_jnl_heads_source  ON jnl_journal_heads(source_system, source_document_id);
CREATE INDEX idx_jnl_lines_journal ON jnl_journal_lines(journal_id);
CREATE INDEX idx_jnl_lines_account ON jnl_journal_lines(account_id);
