-- Posting Engine Domain
CREATE TABLE pst_posting_rules (
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
CREATE TABLE pst_idempotency_keys (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_system     VARCHAR(50)  NOT NULL,
    source_event_id   VARCHAR(200) NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    journal_id        UUID         REFERENCES jnl_journal_heads(id),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PROCESSED' CHECK (status IN ('PROCESSED','FAILED','SKIPPED')),
    error_message     TEXT,
    created_by        VARCHAR(255) NOT NULL DEFAULT 'system',
    processed_at_utc  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (source_system, source_event_id)
);
CREATE INDEX idx_pst_rules_event      ON pst_posting_rules(event_type, priority DESC) WHERE is_active = TRUE;
CREATE INDEX idx_pst_idempotency_key  ON pst_idempotency_keys(source_system, source_event_id);
