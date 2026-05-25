---
name: security-standards
description: "Load when: configuring authentication, authorization, JWT validation, Keycloak integration, role-based access control, secrets management, or reviewing security posture. Tenant isolation is a security boundary — always load with multi-tenant work."
---

# Security Standards

## Objective

Define enterprise-grade security standards for all J4MB platform services.

Security must ensure: tenant isolation, authentication, authorization, traceability, least-privilege access, and secure distributed communication.

---

# Security Philosophy

Security is a core architecture concern — not an afterthought.

Security must be: centralized, explicit, observable, and defense-in-depth oriented.

Zero-trust: never trust client input, frontend validation, internal network assumptions, or external integrations automatically.

Always validate: identity, permissions, tenant ownership, resource access.

---

# Authentication Standards

## Stack

| Component | Choice |
|---|---|
| Identity provider | Keycloak |
| Protocol | OAuth2 + OpenID Connect |
| Token format | JWT (signed) |
| Spring integration | OAuth2 Resource Server |
| Session strategy | Stateless |

Never build custom authentication systems. Add Spring Security only when auth is required.

## Required JWT Claims

```json
{
  "sub": "user-uuid-123",
  "tenant_id": "tenant-acme",
  "tenant_code": "acme",
  "roles": ["ACCOUNT_ADMIN", "ACCOUNTANT"],
  "email": "user@acme.com",
  "preferred_username": "john.doe"
}
```

## Token Validation

Services must validate on every request:
1. Signature (verify against Keycloak public key)
2. Expiration (`exp` claim)
3. Issuer (`iss` claim)
4. Audience (`aud` claim)

Never trust unsigned tokens or malformed claims.

## Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // stateless JWT APIs
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

---

# Keycloak Integration

## Architecture

Single Keycloak realm to start. Realm-per-tenant is future evolution when enterprise isolation is required.

## Tenant Resolution Flow

```
JWT
 ↓
Spring Security (validate signature, expiry, issuer, audience)
 ↓
TenantResolver (extract tenant_id, tenant_code from claims)
 ↓
TenantContext (request-scoped bean)
 ↓
PostgreSQL schema resolution (SET search_path TO tenant_acme)
```

The persistence layer must never trust client-provided schema names or tenant IDs. Tenant context derives exclusively from validated JWT claims.

## Role Mapping

Roles are business-oriented, tenant-scoped, and resolved from JWT claims:

```java
public enum Role {
    SYSTEM_ADMIN,
    ACCOUNT_ADMIN,
    FINANCE_MANAGER,
    ACCOUNTANT,
    DOCUMENT_APPROVER,
    READ_ONLY
}
```

Roles must remain stable. Avoid role explosion. Never trust client-provided role claims that aren't in the validated JWT.

## Service-to-Service Authentication

Use OAuth2 client credentials flow for machine-to-machine communication:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          ledger-service:
            client-id: ${KEYCLOAK_CLIENT_ID}
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            authorization-grant-type: client_credentials
```

## Future Evolution

Architecture supports future:
- Realm-per-tenant for enterprise isolation
- External identity federation (SAML, Azure AD, LDAP)
- SSO integration across tenants
- Tenant-specific identity providers

---

# Authorization Standards

## RBAC + ABAC Hybrid Model

**RBAC** answers: WHO can perform this action?
**ABAC** answers: Under WHICH CONDITIONS is access allowed?

Use both together for comprehensive access control.

## RBAC — Method Security

```java
// Coarse-grained: who can access
@PreAuthorize("hasRole('ACCOUNT_ADMIN')")
public AccountResponse create(CreateAccountRequest request) { ... }

@PreAuthorize("hasAnyRole('ACCOUNTANT', 'FINANCE_MANAGER', 'ACCOUNT_ADMIN')")
public Page<AccountResponse> list(Pageable pageable) { ... }

@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public void suspend(UUID accountId) { ... }
```

## ABAC — Contextual Authorization

For fine-grained access control based on resource attributes:

```java
@Service
public class AccountAuthorizationService {

    public void assertCanAccess(Account account, TenantContext tenantContext) {
        // Tenant ownership check
        if (!account.getTenantId().equals(tenantContext.getTenantId())) {
            throw new AccountNotFoundException(account.getId()); // 404 to prevent leakage
        }
    }

    public void assertCanApprove(Document document, TenantContext tenantContext) {
        // Document ownership + approval limit + workflow state
        assertCanAccess(document, tenantContext);
        if (!document.isPendingApproval()) {
            throw new InvalidWorkflowStateException("Document is not pending approval");
        }
        if (tenantContext.getApprovalLimit().compareTo(document.getAmount()) < 0) {
            throw new ApprovalLimitExceededException(tenantContext.getApprovalLimit(), document.getAmount());
        }
    }
}
```

ABAC use cases: document ownership, approval limits, branch restrictions, workflow state gates.

## Authorization Audit

Authorization decisions must be logged:

```java
log.info("Authorization decision: traceId={}, tenantId={}, userId={}, resource={}, action={}, decision={}",
    traceId, tenantId, userId, resourceType, action, decision);
```

---

# Multi-Tenant Security

Tenant isolation is a security boundary.

Requirements:
- Tenant-aware authorization on every operation
- Schema-level data isolation (PostgreSQL schema-per-tenant)
- Tenant-safe logging (every log includes tenantId)
- No cross-tenant data access paths

Never:
- Trust tenant IDs from request payloads
- Allow cross-tenant queries
- Expose another tenant's resource (return 404, not 403, to prevent tenant existence leakage)

---

# API Security Standards

APIs must:
- Require authentication for all non-public endpoints
- Validate authorization and tenant ownership on every request
- Never expose internal admin endpoints publicly
- Never include stack traces in error responses

Public endpoints (no auth required):
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /api-docs/**`
- `GET /swagger-ui/**`

---

# Secrets Management

## Core Principle

Secrets must NEVER exist in:
- Source code
- Git repositories
- Docker images
- Build artifacts
- CI/CD logs

## Categories

| Secret type | Storage |
|---|---|
| Database passwords | Environment variables or Vault |
| JWT secrets | Environment variables or Vault |
| Keycloak client secret | Environment variables |
| API keys | Environment variables or Vault |
| Certificates | Kubernetes secrets or cert-manager |
| Encryption keys | Vault or KMS |

## Local Development

```dotenv
# .env.sample (committed with placeholders)
SPRING_DATASOURCE_PASSWORD=change-me
KEYCLOAK_CLIENT_SECRET=change-me
JWT_SECRET=change-me-64-chars-base64-encoded

# .env (gitignored, local values only)
SPRING_DATASOURCE_PASSWORD=devpassword123
```

Add to `.gitignore`:
```
.env
.env.*
!.env.sample
```

Never `cat`, print, or expose `.env` contents in logs or AI agent output.

## Secret Rotation

Secrets must support: rotation, expiration, revocation.

Rotation procedure:
1. Generate new secret
2. Update secret store (Vault / Kubernetes secret / Container Apps secret)
3. Trigger rolling restart
4. Verify health
5. Revoke old secret

## CI/CD Pipelines

Pipelines must: inject secrets at runtime (not build time), avoid exposing secrets in logs, use OIDC where possible (no long-lived credentials).

## Logging Prohibition

Never log: passwords, JWT tokens, API keys, certificates, encryption keys, client secrets, full bearer tokens.

---

# Security Logging & Audit

Security operations must log:
- Authentication attempts (success and failure)
- Authorization denials
- Tenant violations
- Token validation failures
- Role changes and privilege escalations

Log format:

```java
log.warn("Authorization denied: traceId={}, tenantId={}, userId={}, resource={}, reason={}",
    MDC.get("traceId"), tenantId, userId, resource, reason);
```

---

# CORS Configuration

For development with frontend dev servers:

```java
@Configuration
@Profile("dev")
public class DevCorsConfig {

    @Bean
    public CorsFilter corsFilter(@Value("${VITE_PORT:5173}") int vitePort) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:" + vitePort));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", source);
        return new CorsFilter(source);
    }
}
```

Never configure CORS permissively in production. In production, the frontend is served from the same origin.

---

# AI Code Generation Rules

AI-generated security code must:
- Validate JWT tokens properly (signature, expiry, issuer, audience)
- Resolve tenant context from JWT claims — never from request payload
- Use `@PreAuthorize` for method-level RBAC
- Implement tenant ownership validation (ABAC) before all resource access
- Support traceability in authorization decisions
- Externalize all secrets via environment variables

Do not generate:
- Insecure token parsing or disabled validation
- Hardcoded JWT secrets or Keycloak credentials
- Unrestricted admin endpoints
- Trust-based authorization (assuming the client-supplied tenantId is safe)
- `http.csrf(csrf -> csrf.disable())` without comment explaining why it's safe for stateless APIs
- Weak role validation (checking for role names without using Spring Security)
