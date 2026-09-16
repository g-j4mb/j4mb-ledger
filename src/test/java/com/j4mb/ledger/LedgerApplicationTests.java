package com.j4mb.ledger;

import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.audit.repository.AuditLogRepository;
import com.j4mb.ledger.balance.repository.AccountBalanceRepository;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.currency.repository.CurrencyRepository;
import com.j4mb.ledger.currency.repository.ExchangeRateRepository;
import com.j4mb.ledger.fiscal.repository.FiscalPeriodRepository;
import com.j4mb.ledger.fiscal.repository.FiscalYearRepository;
import com.j4mb.ledger.journal.repository.JournalHeadRepository;
import com.j4mb.ledger.journal.repository.JournalLineRepository;
import com.j4mb.ledger.platform.TenantRepository;
import com.j4mb.ledger.posting.repository.IdempotencyKeyRepository;
import com.j4mb.ledger.posting.repository.PostingRuleRepository;
import com.j4mb.ledger.shared.context.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    @MockitoBean DataSource dataSource;

    /**
     * JpaRepositoriesAutoConfiguration is excluded (no real DB), so all JPA
     * repositories must be mocked here to satisfy constructor injection in services.
     */
    @MockitoBean TenantRepository          tenantRepository;
    @MockitoBean AccountRepository         accountRepository;
    @MockitoBean CoaNodeRepository         coaNodeRepository;
    @MockitoBean AccountBalanceRepository  accountBalanceRepository;
    @MockitoBean AuditLogRepository        auditLogRepository;
    @MockitoBean FiscalYearRepository      fiscalYearRepository;
    @MockitoBean FiscalPeriodRepository    fiscalPeriodRepository;
    @MockitoBean JournalHeadRepository     journalHeadRepository;
    @MockitoBean JournalLineRepository     journalLineRepository;
    @MockitoBean CurrencyRepository        currencyRepository;
    @MockitoBean ExchangeRateRepository    exchangeRateRepository;
    @MockitoBean PostingRuleRepository     postingRuleRepository;
    @MockitoBean IdempotencyKeyRepository  idempotencyKeyRepository;

    /**
     * UserContext is request-scoped; outside an active HTTP request the CGLIB proxy
     * throws ScopeNotActiveException. Services that inject UserContext (CoaService,
     * AccountService, etc.) will fail to wire unless we provide a flat mock here.
     */
    @MockitoBean UserContext userContext;

    @Test
    void contextLoads() {
        // Verifies that the Spring context wires all beans successfully.
        // Any misconfiguration (circular deps, missing beans, wrong types) fails here.
    }
}
