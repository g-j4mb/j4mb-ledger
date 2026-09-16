package com.j4mb.ledger.provisioning;

import com.j4mb.ledger.platform.Tenant;
import com.j4mb.ledger.platform.TenantRepository;
import com.j4mb.ledger.platform.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantProvisioningService")
class TenantProvisioningServiceTest {

    @Mock TenantRepository   tenantRepository;
    @Mock SchemaProvisioner  schemaProvisioner;
    @Mock TenantMigrator     tenantMigrator;

    @InjectMocks TenantProvisioningService service;

    private TenantProvisioningRequest request;

    @BeforeEach
    void setUp() {
        request = new TenantProvisioningRequest("acme", "Acme Corp", "system");
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("provisions tenant: creates schema, runs migrations, saves tenant row")
    void provision_happyPath() throws Exception { // verify(schemaProvisioner).createSchema() propagates SQLException
        when(tenantRepository.existsByTenantCode("acme")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantProvisioningResult result = service.provision(request);

        // Schema created with correct name
        verify(schemaProvisioner).createSchema("tenant_acme");

        // Flyway ran against the same schema
        verify(tenantMigrator).migrate("tenant_acme");

        // Tenant row saved
        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.getTenantCode()).isEqualTo("acme");
        assertThat(saved.getSchemaName()).isEqualTo("tenant_acme");
        assertThat(saved.getName()).isEqualTo("Acme Corp");
        assertThat(saved.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        // Result mirrors saved entity
        assertThat(result.tenantCode()).isEqualTo("acme");
        assertThat(result.schemaName()).isEqualTo("tenant_acme");
        assertThat(result.tenantName()).isEqualTo("Acme Corp");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.tenantId()).isNotNull();
        assertThat(result.provisionedAt()).isNotNull();
    }

    @Test
    @DisplayName("converts hyphens to underscores when deriving schema name")
    void provision_hyphenatedCode_derivesSchemaWithUnderscores() throws Exception {
        TenantProvisioningRequest hyphenRequest =
            new TenantProvisioningRequest("acme-corp", "Acme Corp", "system");

        when(tenantRepository.existsByTenantCode("acme-corp")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantProvisioningResult result = service.provision(hyphenRequest);

        verify(schemaProvisioner).createSchema("tenant_acme_corp");
        verify(tenantMigrator).migrate("tenant_acme_corp");
        assertThat(result.schemaName()).isEqualTo("tenant_acme_corp");
    }

    // -------------------------------------------------------------------------
    // Duplicate guard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("throws TenantAlreadyExistsException when tenantCode is already registered")
    void provision_duplicateTenantCode_throwsAlreadyExists() throws Exception {
        when(tenantRepository.existsByTenantCode("acme")).thenReturn(true);

        assertThatThrownBy(() -> service.provision(request))
            .isInstanceOf(TenantAlreadyExistsException.class)
            .hasMessageContaining("acme");

        // No DDL or Flyway attempted
        verify(schemaProvisioner, never()).createSchema(any());
        verify(tenantMigrator, never()).migrate(any());
    }

    // -------------------------------------------------------------------------
    // Rollback on schema creation failure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("drops schema and throws TenantProvisioningException when createSchema fails")
    void provision_schemaCreationFails_rollsBackAndThrows() throws Exception {
        when(tenantRepository.existsByTenantCode("acme")).thenReturn(false);
        doThrow(new SQLException("permission denied"))
            .when(schemaProvisioner).createSchema("tenant_acme");

        assertThatThrownBy(() -> service.provision(request))
            .isInstanceOf(TenantProvisioningException.class)
            .hasMessageContaining("acme")
            .hasCauseInstanceOf(SQLException.class);

        verify(schemaProvisioner).dropSchemaQuietly("tenant_acme");
        verify(tenantRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Rollback on migration failure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("drops schema and throws TenantProvisioningException when migration fails")
    void provision_migrationFails_rollsBackAndThrows() throws Exception {
        when(tenantRepository.existsByTenantCode("acme")).thenReturn(false);
        doThrow(new RuntimeException("Flyway migration error"))
            .when(tenantMigrator).migrate("tenant_acme");

        assertThatThrownBy(() -> service.provision(request))
            .isInstanceOf(TenantProvisioningException.class)
            .hasMessageContaining("acme");

        verify(schemaProvisioner).createSchema("tenant_acme");
        verify(schemaProvisioner).dropSchemaQuietly("tenant_acme");
        verify(tenantRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // No rollback on JPA save failure (schema already migrated)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("does NOT drop schema when JPA save fails — schema is migrated; log for manual fix")
    void provision_jpaFails_doesNotDropSchema() throws Exception {
        when(tenantRepository.existsByTenantCode("acme")).thenReturn(false);
        when(tenantRepository.save(any())).thenThrow(new RuntimeException("DB write error"));

        assertThatThrownBy(() -> service.provision(request))
            .isInstanceOf(TenantProvisioningException.class)
            .hasMessageContaining("acme");

        // Schema was created and migrated — do NOT drop
        verify(schemaProvisioner).createSchema("tenant_acme");
        verify(tenantMigrator).migrate("tenant_acme");
        verify(schemaProvisioner, never()).dropSchemaQuietly(any());
    }

    // -------------------------------------------------------------------------
    // deriveSchemaName static helper
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deriveSchemaName: lowercase + prefix")
    void deriveSchemaName_lowercase() {
        assertThat(TenantProvisioningService.deriveSchemaName("ACME"))
            .isEqualTo("tenant_acme");
    }

    @Test
    @DisplayName("deriveSchemaName: hyphens become underscores")
    void deriveSchemaName_hyphenToUnderscore() {
        assertThat(TenantProvisioningService.deriveSchemaName("acme-global-corp"))
            .isEqualTo("tenant_acme_global_corp");
    }
}
