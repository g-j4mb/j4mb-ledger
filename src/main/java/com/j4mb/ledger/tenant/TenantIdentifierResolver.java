package com.j4mb.ledger.tenant;

import com.j4mb.commons.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.stereotype.Component;

/**
 * Hibernate SPI: resolves the current tenant identifier (PostgreSQL schema name)
 * for every session opened by the ORM.
 *
 * <p>Returns {@link #FALLBACK_SCHEMA} when no tenant context is available:
 * <ul>
 *   <li>During Hibernate bootstrap at startup (SessionFactory build, schema validation)</li>
 *   <li>For public/actuator endpoints that carry no {@code X-Tenant-Code} header</li>
 * </ul>
 *
 * <p>The fallback {@code "platform"} schema contains only cross-tenant administrative
 * data ({@code platform.tenants}, {@code platform.users}) — no business data leaks.
 *
 * <p>Injected with the request-scoped {@link TenantContext} as a CGLIB proxy.
 * Spring resolves the actual request-scoped instance on every call, so injection
 * into this singleton bean is safe for in-request use. Outside an active request
 * (Hibernate bootstrap, {@code @Scheduled}, async tasks), the proxy throws
 * {@link ScopeNotActiveException}; that is caught here and the fallback schema is returned.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    /** Schema used when no tenant context exists (bootstrap, public endpoints). */
    static final String FALLBACK_SCHEMA = "platform";

    private final TenantContext tenantContext;

    public TenantIdentifierResolver(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * Returns the PostgreSQL schema name for the current tenant, e.g. {@code "tenant_acme"}.
     * Falls back to {@code "platform"} when no tenant context is active.
     *
     * <p>The {@code ScopeNotActiveException} guard is necessary because Hibernate calls this
     * method during {@code SessionFactory} bootstrap (schema validation) before any HTTP
     * request exists. The scoped proxy throws in that case; we catch it and return the
     * fallback schema so startup succeeds.
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        try {
            if (tenantContext.isInitialized()) {
                return tenantContext.getSchemaName();
            }
        } catch (ScopeNotActiveException ignored) {
            // No active request scope: Hibernate bootstrap, @Scheduled, async threads, etc.
            // Fall through to return the platform fallback.
        }
        return FALLBACK_SCHEMA;
    }

    /**
     * Returns {@code false} so Hibernate does not throw when the tenant identifier
     * changes between session reuses (safe for async/batch tasks).
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
