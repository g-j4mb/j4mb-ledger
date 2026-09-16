-- Account Domain
CREATE TABLE acc_accounts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    coa_node_id     UUID            NOT NULL REFERENCES coa_nodes(id),
    account_number  VARCHAR(30)     NOT NULL UNIQUE,
    name            VARCHAR(200)    NOT NULL,
    currency_code   CHAR(3)         NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    overdraft_limit NUMERIC(20,4)   NOT NULL DEFAULT 0,
    external_ref    VARCHAR(100),
    description     TEXT,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(255)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(255)    NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_acc_accounts_coa    ON acc_accounts(coa_node_id);
CREATE INDEX idx_acc_accounts_status ON acc_accounts(status);
