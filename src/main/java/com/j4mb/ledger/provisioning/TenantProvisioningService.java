package com.j4mb.ledger.provisioning;

import com.j4mb.ledger.platform.Tenant;
import com.j4mb.ledger.platform.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the end-to-end provisioning of a new tenant.
 *
 * <p>Provisioning is a three-step process that crosses two transactional boundaries:
 * <ol>
 *   <li>DDL — {@link SchemaProvisioner#createSchema} runs outside any JPA transaction.</li>
 *   <li>Flyway — {@link TenantMigrator#migrate} runs its own internal JDBC transactions.</li>
 *   <li>JPA — {@link TenantRepository#save} persists the tenant record in {@code platform.tenants}
 *       inside a Spring-managed JPA transaction (committed on method return).</li>
 * </ol>
 *
 * <p>If steps 1 or 2 fail, {@link SchemaProvisioner#dropSchemaQuietly} attempts to clean up the
 * newly created schema so the tenant code remains available for retry. If step 3 fails, the schema
 * exists but is unregistered — an operator must either re-run provisioning or manually drop the
 * orphaned schema.
 *
 * <p>This method is intentionally <em>not</em> annotated with {@code @Transactional} on the outer
 * method because DDL and Flyway cannot participate in a JPA transaction.
 */
@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);

    private final TenantRepository   tenantRepository;
    private final SchemaProvisioner  schemaProvisioner;
    private final TenantMigrator     tenantMigrator;

    TenantProvisioningService(TenantRepository  tenantRepository,
                              SchemaProvisioner schemaProvisioner,
                              TenantMigrator    tenantMigrator) {
        this.tenantRepository  = tenantRepository;
        this.schemaProvisioner = schemaProvisioner;
        this.tenantMigrator    = tenantMigrator;
    }

    /**
     * Provisions a new tenant: creates its PostgreSQL schema, runs Flyway migrations, and
     * registers the tenant in {@code platform.tenants}.
     *
     * @param request validated provisioning request
     * @return result containing the stable tenant ID, code, schema name and timestamps
     * @throws TenantAlreadyExistsException if the {@code tenantCode} is already registered
     * @throws TenantProvisioningException  if schema creation or migration fails
     */
    public TenantProvisioningResult provision(TenantProvisioningRequest request) {
        String tenantCode = request.tenantCode();
        String schemaName = deriveSchemaName(tenantCode);

        log.info("Starting provisioning for tenant '{}' → schema '{}'", tenantCode, schemaName);

        // Guard: reject duplicates before touching the database
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new TenantAlreadyExistsException(tenantCode);
        }

        try {
            // Step 1: DDL — create the PostgreSQL schema
            schemaProvisioner.createSchema(schemaName);

            // Step 2: Flyway — apply all tenant migrations (V1, V2, V3 …) to the new schema
            tenantMigrator.migrate(schemaName);

        } catch (Exception e) {
            log.error("Provisioning failed for tenant '{}' at schema setup; rolling back schema",
                tenantCode, e);
            schemaProvisioner.dropSchemaQuietly(schemaName);
            throw new TenantProvisioningException(tenantCode, e);
        }

        // Step 3: JPA — register tenant in platform.tenants (auto-committed by Spring TX)
        Tenant tenant = Tenant.provision(tenantCode, schemaName, request.tenantName(),
                                         request.provisionedBy());
        try {
            tenant = tenantRepository.save(tenant);
        } catch (Exception e) {
            // Schema exists but tenant row failed — log clearly for manual intervention
            log.error("Tenant row insert failed for '{}' — schema '{}' was created but " +
                      "is unregistered. Manual cleanup or re-run required.",
                tenantCode, schemaName, e);
            throw new TenantProvisioningException(tenantCode, e);
        }

        log.info("Tenant '{}' provisioned successfully: id={}, schema={}",
            tenant.getTenantCode(), tenant.getId(), tenant.getSchemaName());

        return TenantProvisioningResult.from(tenant);
    }

    /**
     * Derives the PostgreSQL schema name from a tenant code.
     * Hyphens are replaced with underscores to produce a valid unquoted identifier.
     * Example: {@code "acme-corp"} → {@code "tenant_acme_corp"}.
     */
    static String deriveSchemaName(String tenantCode) {
        return "tenant_" + tenantCode.toLowerCase().replace('-', '_');
    }
}
