package com.j4mb.ledger.provisioning;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Runs Flyway tenant migrations programmatically against a specific schema.
 *
 * <p>Unlike the platform migrations (which run automatically at startup via
 * Spring Boot's Flyway auto-configuration), tenant migrations are run once
 * per tenant at provisioning time. Each tenant schema gets its own
 * {@code flyway_schema_history} table to track its migration version independently.
 *
 * <p>Migration files are resolved from {@code classpath:db/migration/tenant/}.
 * Flyway sets {@code search_path} to the target schema before executing each file,
 * so migration SQL uses unqualified table names (no schema prefix needed).
 *
 * <h3>Adding new tenant schema columns / tables in future</h3>
 * Drop a new versioned file into {@code db/migration/tenant/} (e.g.
 * {@code V4__add_cost_centres.sql}). On next deployment, existing tenants are
 * automatically migrated by calling {@link #migrate(String)} during a
 * migration sweep job (to be added). New tenants get it as part of provisioning.
 */
@Component
class TenantMigrator {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrator.class);

    private static final String TENANT_MIGRATION_LOCATION = "classpath:db/migration/tenant";

    private final DataSource dataSource;

    TenantMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Applies all pending tenant migrations to the given schema.
     * Safe to call on an already-migrated schema — Flyway is idempotent.
     *
     * @param schemaName the PostgreSQL schema to migrate, e.g. {@code "tenant_acme"}
     */
    void migrate(String schemaName) {
        log.info("Running tenant migrations against schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            // Sets search_path to schemaName and creates flyway_schema_history there
            .schemas(schemaName)
            .locations(TENANT_MIGRATION_LOCATION)
            // Each tenant schema tracks its own migration history
            .table("flyway_schema_history")
            // Strict: no out-of-order migrations, validate checksums
            .outOfOrder(false)
            .validateOnMigrate(true)
            .load();

        MigrateResult result = flyway.migrate();

        log.info("Tenant migrations complete: schema={}, applied={}, current={}",
            schemaName, result.migrationsExecuted, result.targetSchemaVersion);
    }
}
