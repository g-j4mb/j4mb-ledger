package com.j4mb.ledger.provisioning;

import com.j4mb.ledger.platform.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Result returned after successfully provisioning a tenant.
 *
 * @param tenantId    Stable UUID for this tenant.
 * @param tenantCode  Short code used as the gateway header value.
 * @param schemaName  PostgreSQL schema created for this tenant.
 * @param tenantName  Human-readable name.
 * @param status      Current status (always {@code "ACTIVE"} on successful provisioning).
 * @param provisionedAt Timestamp when the tenant was created.
 */
public record TenantProvisioningResult(
    @Schema(description = "Stable UUID for this tenant.", example = "550e8400-e29b-41d4-a716-446655440000") UUID    tenantId,
    @Schema(description = "Short tenant code used as the X-Tenant-Code gateway header.", example = "acme") String  tenantCode,
    @Schema(description = "PostgreSQL schema created for this tenant.", example = "tenant_acme") String  schemaName,
    @Schema(description = "Human-readable tenant name.", example = "Acme Corporation") String  tenantName,
    @Schema(description = "Current tenant status.", example = "ACTIVE") String  status,
    @Schema(description = "Timestamp when the tenant was provisioned (UTC).") Instant provisionedAt
) {

    public static TenantProvisioningResult from(Tenant tenant) {
        return new TenantProvisioningResult(
            tenant.getId(),
            tenant.getTenantCode(),
            tenant.getSchemaName(),
            tenant.getName(),
            tenant.getStatus().name(),
            tenant.getCreatedAt()
        );
    }
}
