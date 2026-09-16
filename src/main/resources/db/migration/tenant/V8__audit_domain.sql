-- Audit Domain — INSERT-ONLY; never UPDATE or DELETE
CREATE TABLE aud_audit_logs (
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
CREATE INDEX idx_aud_logs_entity       ON aud_audit_logs(entity_type, entity_id);
CREATE INDEX idx_aud_logs_performed_at ON aud_audit_logs(performed_at_utc DESC);
