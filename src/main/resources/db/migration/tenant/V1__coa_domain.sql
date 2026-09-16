-- COA Domain — Chart of Accounts hierarchy
CREATE TABLE coa_nodes (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id             UUID         REFERENCES coa_nodes(id),
    code                  VARCHAR(50)  NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    full_path             VARCHAR(500) NOT NULL,
    depth                 INT          NOT NULL DEFAULT 0,
    account_type          VARCHAR(30)  NOT NULL CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    normal_balance        VARCHAR(10)  NOT NULL CHECK (normal_balance IN ('DEBIT','CREDIT')),
    is_postable           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_frozen             BOOLEAN      NOT NULL DEFAULT FALSE,
    description           TEXT,
    display_order         INT          NOT NULL DEFAULT 0,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_by            VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by            VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at_utc        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (code)
);
CREATE INDEX idx_coa_nodes_parent ON coa_nodes(parent_id);
CREATE INDEX idx_coa_nodes_type   ON coa_nodes(account_type);
