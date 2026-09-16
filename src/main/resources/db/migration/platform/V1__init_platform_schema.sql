-- =============================================================================
-- V1__init_platform_schema.sql
--
-- Creates the platform schema: cross-tenant administrative tables.
--
-- NOTE: Replaces the original placeholder V1__init.sql (two comment lines, no SQL).
-- If Flyway was previously applied to a database with that placeholder, run:
--   mvn flyway:repair
-- or recreate the dev database (expected for this greenfield project).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS platform;

-- -----------------------------------------------------------------------------
-- platform.tenants
--
-- Registry of all provisioned tenants.
-- Each tenant maps to exactly one PostgreSQL schema (schema_name).
-- -----------------------------------------------------------------------------
CREATE TABLE platform.tenants
(
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_code  VARCHAR(50)  NOT NULL,
    schema_name  VARCHAR(63)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(255) NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by   VARCHAR(255) NOT NULL,

    CONSTRAINT pk_tenants        PRIMARY KEY (id),
    CONSTRAINT uq_tenants_code   UNIQUE (tenant_code),
    CONSTRAINT uq_tenants_schema UNIQUE (schema_name),
    CONSTRAINT ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEPROVISIONED'))
);

COMMENT ON TABLE  platform.tenants             IS 'Tenant registry — one row per provisioned tenant';
COMMENT ON COLUMN platform.tenants.tenant_code IS 'Short alphanumeric identifier injected by gateway, e.g. acme';
COMMENT ON COLUMN platform.tenants.schema_name IS 'PostgreSQL schema owned by this tenant, e.g. tenant_acme';
COMMENT ON COLUMN platform.tenants.status      IS 'ACTIVE | SUSPENDED | DEPROVISIONED';

-- -----------------------------------------------------------------------------
-- platform.users
--
-- Platform-level user records linked to Keycloak subjects.
-- One row per Keycloak user; tenant-scoped via tenant_id FK.
-- -----------------------------------------------------------------------------
CREATE TABLE platform.users
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    keycloak_subject    VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    preferred_username  VARCHAR(255) NOT NULL,
    roles               TEXT[]       NOT NULL DEFAULT '{}',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(255) NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by          VARCHAR(255) NOT NULL,

    CONSTRAINT pk_users                  PRIMARY KEY (id),
    CONSTRAINT uq_users_keycloak_subject UNIQUE (keycloak_subject),
    CONSTRAINT fk_users_tenant           FOREIGN KEY (tenant_id)
                                             REFERENCES platform.tenants (id),
    CONSTRAINT ck_users_status           CHECK (status IN ('ACTIVE', 'DISABLED'))
);

COMMENT ON TABLE  platform.users                    IS 'Platform users — linked to Keycloak subjects';
COMMENT ON COLUMN platform.users.keycloak_subject   IS 'Keycloak sub claim — globally unique across all tenants';
COMMENT ON COLUMN platform.users.roles              IS 'Role array from gateway header, e.g. {ACCOUNT_ADMIN,ACCOUNTANT}';

-- -----------------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------------
CREATE INDEX idx_tenants_code   ON platform.tenants (tenant_code);
CREATE INDEX idx_tenants_schema ON platform.tenants (schema_name);
CREATE INDEX idx_users_tenant   ON platform.users (tenant_id);
CREATE INDEX idx_users_email    ON platform.users (email);
