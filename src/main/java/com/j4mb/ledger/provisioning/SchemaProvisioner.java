package com.j4mb.ledger.provisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Executes raw DDL to create and drop PostgreSQL schemas.
 *
 * <p>Schema creation is DDL and runs outside JPA/Hibernate so that it does not
 * interfere with the multi-tenant connection provider. A dedicated JDBC connection
 * is acquired from the pool, used for DDL, and returned immediately.
 *
 * <p>Schema names are validated against an allowlist regex before embedding in SQL
 * (same pattern used by {@link com.j4mb.ledger.tenant.SchemaMultiTenantConnectionProvider}).
 */
@Component
class SchemaProvisioner {

    private static final Logger log = LoggerFactory.getLogger(SchemaProvisioner.class);

    private static final Pattern SAFE_SCHEMA =
        Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");

    private final DataSource dataSource;

    SchemaProvisioner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates a PostgreSQL schema. Idempotent — uses {@code CREATE SCHEMA IF NOT EXISTS}.
     *
     * @param schemaName validated schema name, e.g. {@code "tenant_acme"}
     * @throws SQLException if the DDL fails
     */
    void createSchema(String schemaName) throws SQLException {
        validate(schemaName);
        log.info("Creating schema: {}", schemaName);
        executeDdl("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }

    /**
     * Drops a schema and all its objects. Used as a rollback when provisioning fails.
     * Logs errors but does not throw — called from a cleanup path.
     *
     * @param schemaName validated schema name
     */
    void dropSchemaQuietly(String schemaName) {
        try {
            validate(schemaName);
            log.warn("Rolling back: dropping schema {}", schemaName);
            executeDdl("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
        } catch (Exception e) {
            log.error("Failed to drop schema '{}' during rollback — manual cleanup required",
                schemaName, e);
        }
    }

    private void executeDdl(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement  stmt       = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void validate(String schemaName) {
        if (schemaName == null || !SAFE_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Unsafe schema name: " + schemaName);
        }
    }
}
