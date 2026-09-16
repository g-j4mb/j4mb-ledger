package com.j4mb.ledger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String TENANT_CODE_HEADER = "X-Tenant-Code";
    private static final String BEARER_SCHEME      = "bearerAuth";

    @Bean
    public OpenAPI ledgerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("J4MB Ledger Service API")
                        .description("Double-entry financial ledger for the J4MB ERP platform. " +
                                "All business endpoints require the X-Tenant-Code header injected by the API Gateway after JWT validation.")
                        .version("v1")
                        .contact(new Contact()
                                .name("J4MB Platform Team")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token — validated by the API Gateway. " +
                                        "The gateway strips the token and injects X-Tenant-Code before forwarding to this service.")));
    }

    // -------------------------------------------------------------------------
    // API groups
    // -------------------------------------------------------------------------

    @Bean
    public GroupedOpenApi internalAdminApi() {
        return GroupedOpenApi.builder()
                .group("internal-admin")
                .displayName("Internal Admin")
                .pathsToMatch("/internal/**")
                .build();
    }

    @Bean
    public GroupedOpenApi currencyApi() {
        return GroupedOpenApi.builder()
                .group("currencies")
                .displayName("Currencies")
                .pathsToMatch("/api/v1/currencies/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi fiscalApi() {
        return GroupedOpenApi.builder()
                .group("fiscal")
                .displayName("Fiscal")
                .pathsToMatch("/api/v1/fiscal/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi coaApi() {
        return GroupedOpenApi.builder()
                .group("coa")
                .displayName("Chart of Accounts")
                .pathsToMatch("/api/v1/coa/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi accountsApi() {
        return GroupedOpenApi.builder()
                .group("accounts")
                .displayName("Accounts")
                .pathsToMatch("/api/v1/accounts/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi journalsApi() {
        return GroupedOpenApi.builder()
                .group("journals")
                .displayName("Journals")
                .pathsToMatch("/api/v1/journals/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi closingApi() {
        return GroupedOpenApi.builder()
                .group("closing")
                .displayName("Period & Year Closing")
                .pathsToMatch("/api/v1/fiscal/*/close", "/api/v1/fiscal/*/reopen", "/api/v1/fiscal/*/lock")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi exchangeRatesApi() {
        return GroupedOpenApi.builder()
                .group("exchange-rates")
                .displayName("Exchange Rates")
                .pathsToMatch("/api/v1/exchange-rates/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi postingRulesApi() {
        return GroupedOpenApi.builder()
                .group("posting-rules")
                .displayName("Posting Rules")
                .pathsToMatch("/api/v1/posting-rules/**")
                .addOperationCustomizer(tenantCodeHeaderCustomizer())
                .build();
    }

    // -------------------------------------------------------------------------
    // Adds X-Tenant-Code as a required header to every operation in a group
    // -------------------------------------------------------------------------

    private OperationCustomizer tenantCodeHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name(TENANT_CODE_HEADER)
                    .description("Tenant identifier. Injected by the API Gateway after validating the JWT. " +
                            "Routes all database queries to the tenant's isolated PostgreSQL schema.")
                    .required(true)
                    .schema(new StringSchema()
                            .example("acme")
                            .pattern("^[a-z0-9][a-z0-9-]*$")));
            return operation;
        };
    }
}
