package com.j4mb.ledger.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j4mb.ledger.shared.context.UserContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantProvisioningController.class)
@DisplayName("TenantProvisioningController")
class TenantProvisioningControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TenantProvisioningService provisioningService;
    @MockitoBean UserContext               userContext;

    private static final UUID     TENANT_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant  PROVISIONED = Instant.parse("2026-01-15T10:00:00Z");

    // -------------------------------------------------------------------------
    // 201 Created — happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /internal/admin/tenants → 201 Created with body and Location header")
    void provision_validRequest_returns201() throws Exception {
        TenantProvisioningResult result = new TenantProvisioningResult(
            TENANT_ID, "acme", "tenant_acme", "Acme Corp", "ACTIVE", PROVISIONED);
        when(provisioningService.provision(any())).thenReturn(result);

        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "acme",
                        "tenantName": "Acme Corp",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location",
                endsWith("/internal/admin/tenants/" + TENANT_ID)))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.tenantCode").value("acme"))
            .andExpect(jsonPath("$.data.schemaName").value("tenant_acme"))
            .andExpect(jsonPath("$.data.tenantName").value("Acme Corp"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.tenantId").value(TENANT_ID.toString()));
    }

    // -------------------------------------------------------------------------
    // 409 Conflict — duplicate tenant code
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /internal/admin/tenants → 409 when tenantCode already exists")
    void provision_duplicateCode_returns409() throws Exception {
        when(provisioningService.provision(any()))
            .thenThrow(new TenantAlreadyExistsException("acme"));

        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "acme",
                        "tenantName": "Acme Corp",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("TENANT_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.error.message").value(containsString("acme")));
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — provisioning infrastructure failure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /internal/admin/tenants → 500 when provisioning infrastructure fails")
    void provision_infrastructureFailure_returns500() throws Exception {
        when(provisioningService.provision(any()))
            .thenThrow(new TenantProvisioningException("acme",
                new RuntimeException("schema creation failed")));

        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "acme",
                        "tenantName": "Acme Corp",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("PROVISIONING_FAILED"))
            .andExpect(jsonPath("$.error.message").value(containsString("acme")));
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request — bean validation failures
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /internal/admin/tenants → 400 when tenantCode is blank")
    void provision_blankTenantCode_returns400() throws Exception {
        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "",
                        "name": "Acme Corp",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/admin/tenants → 400 when tenantCode has uppercase letters")
    void provision_uppercaseTenantCode_returns400() throws Exception {
        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "ACME",
                        "name": "Acme Corp",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/admin/tenants → 400 when tenantCode has invalid characters")
    void provision_invalidCharactersInCode_returns400() throws Exception {
        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "acme corp",
                        "name": "Acme Corp",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/admin/tenants → 400 when name is blank")
    void provision_blankName_returns400() throws Exception {
        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "acme",
                        "name": "",
                        "provisionedBy": "system"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/admin/tenants → 400 when provisionedBy is blank")
    void provision_blankProvisionedBy_returns400() throws Exception {
        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantCode": "acme",
                        "name": "Acme Corp",
                        "provisionedBy": ""
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/admin/tenants → 400 when request body is missing required fields")
    void provision_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/internal/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
