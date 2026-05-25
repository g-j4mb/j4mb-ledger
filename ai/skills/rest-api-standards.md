---
name: rest-api-standards
description: "Load when: designing REST endpoints, generating controllers, defining response formats, or reviewing API contracts. APIs are long-term contracts — apply these standards before writing the first endpoint."
---

# REST API Standards

## Objective

Define enterprise-grade REST API standards for all J4MB backend services. APIs must be consistent, predictable, versioned, and integration-stable.

---

# API Design Principles

APIs must be: resource-oriented, versioned, predictable, self-descriptive, stateless.

APIs are long-term contracts. Avoid breaking changes whenever possible. Prefer additive evolution.

---

# Resource Naming

Use plural nouns, lowercase paths, hyphen-separated when needed.

```
/api/v1/accounts          ✅
/api/v1/payments          ✅
/api/v1/journal-entries   ✅

/getAccounts              ❌  no verbs
/createPayment            ❌  no verbs
/accountService           ❌  no service suffix
```

---

# Versioning

All APIs must be versioned via URI:

```
/api/v1/accounts
/api/v2/payments
```

Never deploy unversioned production APIs.

---

# HTTP Method Semantics

| Method | Use | Must be idempotent? |
|---|---|---|
| GET | Retrieve resource, no state change | Yes |
| POST | Create resource or trigger non-idempotent operation | No |
| PUT | Replace entire resource | Yes |
| PATCH | Partial update | Ideally |
| DELETE | Remove resource | Yes where possible |

---

# Standard Response Formats

All responses follow these structures. Never return raw entity objects.

## Success

```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-05-23T12:00:00Z"
}
```

## Paginated list

```json
{
  "success": true,
  "data": [],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6
  },
  "timestamp": "2026-05-23T12:00:00Z"
}
```

## Error

```json
{
  "success": false,
  "error": {
    "code": "ACCOUNT_NOT_FOUND",
    "message": "Account not found",
    "details": []
  },
  "timestamp": "2026-05-23T12:00:00Z",
  "traceId": "abc123"
}
```

## Validation error

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "email", "message": "must be a valid email" }
    ]
  },
  "timestamp": "2026-05-23T12:00:00Z",
  "traceId": "abc123"
}
```

---

# HTTP Status Code Standards

| Status | When |
|---|---|
| 200 OK | Successful GET, PUT, PATCH |
| 201 Created | Successful POST that creates a resource |
| 204 No Content | Successful DELETE or action with no body |
| 400 Bad Request | Validation failure or malformed payload |
| 401 Unauthorized | Missing or invalid authentication |
| 403 Forbidden | Authenticated but not authorized |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Business conflict (duplicate resource, optimistic lock) |
| 422 Unprocessable Entity | Business rule validation failure |
| 500 Internal Server Error | Unexpected server failure |

Never return 200 for failures. Never expose stack traces, SQL errors, or framework internals.

---

# Pagination Standards

All list endpoints must support pagination. Never return unbounded result sets.

Query parameters:

```
GET /api/v1/accounts?page=0&size=20&sort=createdAt,desc
GET /api/v1/accounts?status=ACTIVE&page=0&size=20
```

---

# Request Validation

All request bodies must be validated with Jakarta Validation:

```java
public record CreateAccountRequest(
    @NotBlank String accountName,
    @NotNull UUID tenantId,
    @NotBlank @Size(min = 3, max = 10) String currencyCode
) {}

@PostMapping
public ResponseEntity<ApiResponse<AccountResponse>> create(
        @Valid @RequestBody CreateAccountRequest request) {
    // ...
}
```

Validation failures must return structured `VALIDATION_ERROR` responses with field-level details.

---

# Idempotency

Critical operations must support idempotency where duplicates would cause harm:
- Payment processing
- Ledger posting
- Transaction submission

Use `Idempotency-Key` header and unique database constraints.

---

# Multi-Tenant API Standards

APIs must operate within authenticated tenant context. Tenant context resolves from JWT claims — never from request payload.

Never trust client-supplied `tenantId` query parameters or request body fields.

---

# Security Standards

APIs must:
- Require authentication for all non-public endpoints
- Enforce authorization and validate tenant ownership
- Never expose internal admin endpoints publicly
- Never include stack traces in error responses

---

# OpenAPI Documentation

All APIs must generate OpenAPI documentation via `springdoc-openapi`.

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

Documentation must include: endpoint descriptions, request examples, response examples, error response examples.

---

# Async API Patterns

Long-running operations should support asynchronous processing:
- Submit the job (POST → 202 Accepted + job ID)
- Poll for status (GET /jobs/{jobId})
- Event-driven completion via Kafka

Examples: document processing, bulk imports, report generation.

---

# API Evolution Principles

Prefer:
- Backward-compatible changes (additive only)
- New versions only for breaking changes
- Stability over time

Avoid:
- Removing fields from responses without versioning
- Changing response structure without a version bump
- Changing existing field semantics

---

# AI Code Generation Rules

AI-generated APIs must:
- Follow REST semantics strictly
- Generate structured `ApiResponse<T>` wrapper responses
- Use proper HTTP status codes
- Include Jakarta Validation on all request DTOs
- Support traceability via `traceId` in error responses
- Include pagination for all list endpoints

Do not generate:
- Raw entity returns in controller responses
- Inconsistent or ad-hoc response structures
- Generic unversioned endpoints
- Unvalidated request bodies
- Hardcoded tenant IDs
