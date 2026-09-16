-- Balance Domain — per-period balance cache
CREATE TABLE bal_account_balances (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID            NOT NULL REFERENCES acc_accounts(id),
    fiscal_period_id  UUID            NOT NULL REFERENCES fsc_periods(id),
    currency_code     CHAR(3)         NOT NULL,
    opening_debit     NUMERIC(20,4)   NOT NULL DEFAULT 0,
    opening_credit    NUMERIC(20,4)   NOT NULL DEFAULT 0,
    period_debit      NUMERIC(20,4)   NOT NULL DEFAULT 0,
    period_credit     NUMERIC(20,4)   NOT NULL DEFAULT 0,
    closing_debit     NUMERIC(20,4)   NOT NULL DEFAULT 0,
    closing_credit    NUMERIC(20,4)   NOT NULL DEFAULT 0,
    last_rebuilt_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255)    NOT NULL DEFAULT 'system',
    updated_by        VARCHAR(255)    NOT NULL DEFAULT 'system',
    version           BIGINT          NOT NULL DEFAULT 0,
    UNIQUE (account_id, fiscal_period_id, currency_code)
);
CREATE INDEX idx_bal_balances_account ON bal_account_balances(account_id);
CREATE INDEX idx_bal_balances_period  ON bal_account_balances(fiscal_period_id);
