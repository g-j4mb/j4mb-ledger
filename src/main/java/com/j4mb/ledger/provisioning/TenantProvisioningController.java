package com.j4mb.ledger.provisioning;

import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Tag(name = "Internal Admin", description = "Tenant lifecycle management. Internal network only — not exposed via the public API Gateway.")
@RestController
@RequestMapping("/internal/admin/tenants")
class TenantProvisioningController {

    private final TenantProvisioningService provisioningService;

    TenantProvisioningController(TenantProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Operation(
        summary = "Provision a new tenant",
        description = "Creates a PostgreSQL schema for the tenant, runs all Flyway migrations, and registers the tenant in platform.tenants. Idempotent on schema creation but returns 409 on duplicate tenantCode.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant provisioned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure — tenantCode format or missing fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "tenantCode already registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Schema creation or Flyway migration failed")
    })
    @PostMapping
    ResponseEntity<ApiResponse<TenantProvisioningResult>> provision(
            @Valid @RequestBody TenantProvisioningRequest request,
            UriComponentsBuilder uriBuilder) {

        TenantProvisioningResult result = provisioningService.provision(request);

        URI location = uriBuilder
            .path("/internal/admin/tenants/{id}")
            .buildAndExpand(result.tenantId())
            .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(result));
    }
}
