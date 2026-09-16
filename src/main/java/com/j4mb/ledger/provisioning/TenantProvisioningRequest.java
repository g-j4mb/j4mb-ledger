package com.j4mb.ledger.provisioning;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to provision a new tenant.
 *
 * @param tenantCode    Short alphanumeric identifier. Lowercase letters, digits,
 *                      and hyphens only. Used to derive the PostgreSQL schema name
 *                      ({@code "tenant_" + tenantCode}). Immutable once provisioned.
 * @param tenantName    Human-readable name shown in the UI, e.g. "Acme Corporation".
 * @param provisionedBy Identity that triggered provisioning — user email or system name.
 *                      Stored in {@code created_by} / {@code updated_by} audit fields.
 */
public record TenantProvisioningRequest(

    @Schema(description = "Short alphanumeric tenant identifier. Lowercase letters, digits, and hyphens only. Becomes the PostgreSQL schema prefix.", example = "acme")
    @NotBlank(message = "tenantCode is required")
    @Size(min = 2, max = 50, message = "tenantCode must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[a-z0-9][a-z0-9-]{1,49}$",
        message = "tenantCode must start with a lowercase letter or digit and contain only " +
                  "lowercase letters, digits, and hyphens"
    )
    String tenantCode,

    @Schema(description = "Human-readable tenant name displayed in the UI.", example = "Acme Corporation")
    @NotBlank(message = "tenantName is required")
    @Size(max = 255, message = "tenantName must not exceed 255 characters")
    String tenantName,

    @Schema(description = "Identity that triggered provisioning — user email or system name.", example = "admin@j4mb.com")
    @NotBlank(message = "provisionedBy is required")
    @Size(max = 255)
    String provisionedBy
) {}
