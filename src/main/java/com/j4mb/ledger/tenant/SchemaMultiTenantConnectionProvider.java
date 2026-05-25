package com.j4mb.ledger.tenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Hibernate SPI: provides JDBC connections with the PostgreSQL {@code search_path}
 * set to the current tenant's schema on acquisition, and reset on release.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>{@link #getConnection(String)} acquires a connection from HikariCP,
 *       then executes {@code SET search_path TO <schema>}.</li>
 *   <li>Hibernate runs all SQL for the current transaction through this connection.</li>
 *   <li>{@link #releaseConnection(String, Connection)} resets the schema to
 *       {@code DEFAULT} before returning the connection to the pool, ensuring
 *       no stale {@code search_path} state leaks to the next tenant.</li>
 * </ol>
 *
 * <h3>Performance</h3>
 * {@code SET search_path} is a metadata-only PostgreSQL command (~0.1 ms).
 * With {@link #supportsAggressiveRelease()} returning {@code false}, Hibernate holds
 * one connection per transaction — so the switch happens once per transaction,
 * not per SQL statement.
 *
 * <h3>Safety</h3>
 * Schema names are validated against an allowlist regex before use in SQL to
 * provide defence-in-depth even though names are derived from the gateway header.
 * Reset failures are logged at ERROR and the connection is always returned to the pool.
 */
@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final Logger log =
        LoggerFactory.getLogger(SchemaMultiTenantConnectionProvider.class);

    /**
     * Allowlist: starts with a letter, followed by letters/digits/underscores,
     * total length ≤ 63 chars (PostgreSQL identifier limit).
     */
    private static final Pattern SAFE_SCHEMA =
        Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // -----------------------------------------------------------------------
    // Bootstrap connections — used by Hibernate during SessionFactory build,
    // schema validation, and sequence queries. No schema switching needed.
    // -----------------------------------------------------------------------

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        // HikariCP: close() returns to pool, does not physically close.
        connection.close();
    }

    // -----------------------------------------------------------------------
    // Tenant-specific connections — called per transaction.
    // -----------------------------------------------------------------------

    @Override
    public Connection getConnection(String schema) throws SQLException {
        validateSchema(schema);
        Connection connection = dataSource.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + schema);
        } catch (SQLException e) {
            // Return the connection to the pool before rethrowing — do not leak it.
            connection.close();
            throw new SQLException(
                "Failed to set search_path to schema '" + schema + "'", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String schema, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Reset to the database default before returning to the HikariCP pool.
            // This prevents stale search_path leaking to the next tenant that
            // acquires this physical connection.
            stmt.execute("SET search_path TO DEFAULT");
        } catch (SQLException e) {
            log.error(
                "Failed to reset search_path to DEFAULT after schema='{}'. " +
                "Connection may be in inconsistent state — HikariCP will health-check it.",
                schema, e);
        } finally {
            // Always return to pool regardless of reset outcome.
            connection.close();
        }
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    @Override
    public boolean supportsAggressiveRelease() {
        // false: Hibernate holds the connection for the full transaction duration.
        // Required for search_path switching — the path must stay set across all
        // statements in the transaction.
        return false;
    }

    @Override
    public boolean isUnwrappableAs(@SuppressWarnings("rawtypes") Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException(
            "SchemaMultiTenantConnectionProvider does not support unwrap");
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void validateSchema(String schema) {
        if (schema == null || !SAFE_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException(
                "Unsafe or null schema name rejected: '" + schema + "'. " +
                "Schema names must match ^[a-zA-Z][a-zA-Z0-9_]{0,62}$");
        }
    }
}
