package com.j4mb.ledger.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registry entry for a provisioned tenant.
 *
 * <p>Persisted to {@code platform.tenants}. The explicit {@code schema = "platform"}
 * on {@link Table} causes Hibernate to generate schema-qualified SQL
 * ({@code SELECT ... FROM platform.tenants}), so queries always target the
 * platform schema regardless of the current {@code search_path} set by
 * {@link com.j4mb.ledger.tenant.SchemaMultiTenantConnectionProvider}.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>{@code tenantCode} is immutable after creation</li>
 *   <li>{@code schemaName} is derived from {@code tenantCode} and is immutable</li>
 *   <li>Status transitions are explicit methods — no direct setter</li>
 * </ul>
 */
@Entity
@Table(name = "tenants", schema = "platform")
public class Tenant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_code", nullable = false, updatable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(name = "schema_name", nullable = false, updatable = false, unique = true, length = 63)
    private String schemaName;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    protected Tenant() {
        // JPA requires a no-arg constructor
    }

    /**
     * Factory method — creates a new tenant in {@link TenantStatus#ACTIVE} state.
     * Use this instead of the constructor to enforce business invariants.
     *
     * @param tenantCode     short alphanumeric code, e.g. {@code "acme"}
     * @param schemaName     PostgreSQL schema name, e.g. {@code "tenant_acme"}
     * @param name           human-readable tenant name, e.g. {@code "Acme Corporation"}
     * @param provisionedBy  identity that triggered provisioning (user email or system)
     */
    public static Tenant provision(String tenantCode,
                                   String schemaName,
                                   String name,
                                   String provisionedBy) {
        Tenant tenant = new Tenant();
        tenant.id         = UUID.randomUUID();
        tenant.tenantCode = tenantCode;
        tenant.schemaName = schemaName;
        tenant.name       = name;
        tenant.status     = TenantStatus.ACTIVE;
        tenant.createdAt  = Instant.now();
        tenant.createdBy  = provisionedBy;
        tenant.updatedAt  = tenant.createdAt;
        tenant.updatedBy  = provisionedBy;
        return tenant;
    }

    // -----------------------------------------------------------------------
    // Business methods
    // -----------------------------------------------------------------------

    public void suspend(String updatedBy) {
        if (this.status != TenantStatus.ACTIVE) {
            throw new IllegalStateException(
                "Cannot suspend tenant in status: " + this.status);
        }
        this.status    = TenantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;
    }

    public void resume(String updatedBy) {
        if (this.status != TenantStatus.SUSPENDED) {
            throw new IllegalStateException(
                "Cannot resume tenant in status: " + this.status);
        }
        this.status    = TenantStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;
    }

    public void deprovision(String updatedBy) {
        this.status    = TenantStatus.DEPROVISIONED;
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;
    }

    // -----------------------------------------------------------------------
    // Getters (no public setters — use business methods for state transitions)
    // -----------------------------------------------------------------------

    public UUID          getId()          { return id; }
    public String        getTenantCode()  { return tenantCode; }
    public String        getSchemaName()  { return schemaName; }
    public String        getName()        { return name; }
    public TenantStatus  getStatus()      { return status; }
    public Instant       getCreatedAt()   { return createdAt; }
    public String        getCreatedBy()   { return createdBy; }
    public Instant       getUpdatedAt()   { return updatedAt; }
    public String        getUpdatedBy()   { return updatedBy; }
}
