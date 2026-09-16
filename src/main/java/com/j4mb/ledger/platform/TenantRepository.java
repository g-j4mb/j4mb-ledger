package com.j4mb.ledger.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Tenant} entities in {@code platform.tenants}.
 *
 * <p>All queries are schema-qualified via {@code @Table(schema = "platform")}
 * on the entity — no special handling needed here.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsByTenantCode(String tenantCode);

    Optional<Tenant> findByTenantCode(String tenantCode);
}
