package com.j4mb.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.sql.DataSource;

/**
 * Spring context load test.
 *
 * <p>Verifies that the application context wires all beans without errors —
 * TenantContext, TenantContextFilter, TenantIdentifierResolver,
 * SchemaMultiTenantConnectionProvider, HibernateConfig, etc.
 *
 * <p>JPA, Hibernate bootstrap, and Flyway are disabled via test
 * {@code application.yml}. The {@link DataSource} is mocked to prevent
 * HikariCP connection attempts. Hibernate's {@code EntityManagerFactory}
 * (which calls {@code MultiTenantConnectionProvider.getAnyConnection()} during
 * bootstrap) is excluded so no actual JDBC connection is needed.
 *
 * <p>Tests requiring a live database should use Testcontainers.
 */
@SpringBootTest(
    properties = {
        // Exclude JPA/DataSource auto-configurations: prevents EntityManagerFactory
        // bootstrap which calls MultiTenantConnectionProvider.getAnyConnection()
        // on startup — that call would NPE against a mocked DataSource.
        // JpaRepositoriesAutoConfiguration also excluded as it requires EntityManagerFactory.
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
    }
)
class LedgerApplicationTests {

    /**
     * Mocked DataSource prevents HikariCP from attempting a real DB connection.
     * SchemaMultiTenantConnectionProvider and HibernateConfig still wire up
     * correctly against this mock.
     */
    @MockBean
    DataSource dataSource;

    @Test
    void contextLoads() {
        // Verifies that the Spring context wires all beans successfully.
        // Any misconfiguration (circular deps, missing beans, wrong types) fails here.
    }
}
