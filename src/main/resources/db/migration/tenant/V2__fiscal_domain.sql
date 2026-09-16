-- Fiscal Domain
CREATE TABLE fsc_fiscal_years (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    year_name       VARCHAR(20) NOT NULL UNIQUE,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED','LOCKED')),
    closed_at_utc   TIMESTAMPTZ,
    closed_by       VARCHAR(255),
    version         BIGINT      NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE fsc_periods (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fiscal_year_id  UUID        NOT NULL REFERENCES fsc_fiscal_years(id),
    period_name     VARCHAR(20) NOT NULL,
    period_number   SMALLINT    NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED','LOCKED')),
    closed_at_utc   TIMESTAMPTZ,
    closed_by       VARCHAR(255),
    version         BIGINT      NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at_utc  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (fiscal_year_id, period_number)
);
CREATE INDEX idx_fsc_periods_year  ON fsc_periods(fiscal_year_id);
CREATE INDEX idx_fsc_periods_dates ON fsc_periods(start_date, end_date);
