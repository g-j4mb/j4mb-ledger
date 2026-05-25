package com.j4mb.ledger.config;

import com.j4mb.ledger.tenant.SchemaMultiTenantConnectionProvider;
import com.j4mb.ledger.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Hibernate's schema-per-tenant multi-tenancy SPIs with Spring Boot.
 *
 * <p>Uses {@link HibernatePropertiesCustomizer} — the correct Spring Boot 3.x
 * integration point — so all other JPA properties from {@code application.yml}
 * ({@code spring.jpa.properties.*}) are preserved alongside these additions.
 *
 * <h3>How Hibernate 6.x detects multi-tenancy</h3>
 * In Hibernate 6 the {@code hibernate.multiTenancy=SCHEMA} property is no longer
 * required. Hibernate automatically enables multi-tenancy when both
 * {@link AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER} and
 * {@link AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER} are present.
 *
 * <h3>Request flow</h3>
 * <pre>
 * TenantContextFilter → TenantContext.initialize(code, schema)
 *                          ↓
 *                    TenantIdentifierResolver.resolveCurrentTenantIdentifier()
 *                          ↓  returns "tenant_acme"
 *                    SchemaMultiTenantConnectionProvider.getConnection("tenant_acme")
 *                          ↓  SET search_path TO tenant_acme
 *                    PostgreSQL — all queries within tenant_acme schema
 *                          ↓  on transaction commit / rollback
 *                    SchemaMultiTenantConnectionProvider.releaseConnection(...)
 *                          ↓  SET search_path TO DEFAULT
 *                    HikariCP pool — connection returned in clean state
 * </pre>
 */
@Configuration
public class HibernateConfig {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    public HibernateConfig(SchemaMultiTenantConnectionProvider connectionProvider,
                           TenantIdentifierResolver tenantIdentifierResolver) {
        this.connectionProvider       = connectionProvider;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernateMultiTenancyCustomizer() {
        return properties -> {
            properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                           connectionProvider);
            properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                           tenantIdentifierResolver);
        };
    }
}
