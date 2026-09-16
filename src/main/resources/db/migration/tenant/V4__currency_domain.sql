-- Currency Domain
CREATE TABLE cur_currencies (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    currency_code    CHAR(3)     NOT NULL UNIQUE,
    currency_name    VARCHAR(100) NOT NULL,
    is_base_currency BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    decimal_places   INT         NOT NULL DEFAULT 2,
    created_by       VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE cur_exchange_rates (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency   CHAR(3)         NOT NULL,
    to_currency     CHAR(3)         NOT NULL,
    rate            NUMERIC(20,8)   NOT NULL CHECK (rate > 0),
    rate_type       VARCHAR(20)     NOT NULL DEFAULT 'SPOT' CHECK (rate_type IN ('SPOT','AVERAGE','CLOSING')),
    effective_date  DATE            NOT NULL,
    source          VARCHAR(50),
    created_by      VARCHAR(255)    NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (from_currency, to_currency, rate_type, effective_date)
);
CREATE INDEX idx_cur_rates_lookup ON cur_exchange_rates(from_currency, to_currency, effective_date DESC);
