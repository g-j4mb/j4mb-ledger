---
name: backend-operations
description: "Load when: building exception handling, validation layers, or logging for any service. Covers centralized error responses, multi-layer validation, structured logging, and observability standards. Stack with rest-api-standards for complete API coverage."
---

# Backend Operations Standards

## Objective

Define enterprise-grade exception handling, validation, and logging standards for all J4MB backend services.

---

# Exception Handling

## Philosophy

Exceptions are part of system contracts. Handling must be centralized, predictable, and observable. It must never expose implementation internals.

---

## Exception Categories

| Category | Examples | HTTP Status |
|---|---|---|
| Business | `AccountNotFoundException`, `InsufficientBalanceException` | 404, 409, 422 |
| Validation | Missing fields, invalid formats | 400 |
| Security | Auth failure, tenant access violation | 401, 403 |
| Infrastructure | DB failure, Kafka failure | 500 (internal only) |
| System | Unexpected NPE, unhandled state | 500 |

---

## Centralized Exception Handling

Use `@RestControllerAdvice` for all REST exception handling. Never duplicate exception handling across controllers.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: traceId={}", MDC.get("traceId"), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {
        List<FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: traceId={}, path={}", MDC.get("traceId"),
                request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

---

## Business Exception Standards

Business exceptions must be explicit and use domain terminology:

```java
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super("Insufficient balance: required=" + required + ", available=" + available);
    }
}

public class UnbalancedJournalEntryException extends RuntimeException {
    public UnbalancedJournalEntryException(BigDecimal debit, BigDecimal credit) {
        super("Journal entry must balance: debit=" + debit + ", credit=" + credit);
    }
}
```

---

## Financial Exception Standards

Financial operations require strict traceability:

```java
log.error("Financial operation failed: traceId={}, tenantId={}, operationType={}, amount={}",
    MDC.get("traceId"), tenantId, operationType, amount, ex);
```

Critical financial exceptions: `InsufficientBalanceException`, `DuplicateTransactionException`, `CurrencyMismatchException`, `UnbalancedJournalEntryException`.

---

## Multi-Tenant Exception Standards

Prevent tenant leakage in error responses. When a resource in another tenant exists, return 404 (not 403) to avoid revealing tenant data existence.

---

## Async / Kafka Exception Handling

Kafka consumers must:
- Avoid silent message loss
- Support dead-letter queues
- Be replay-safe (idempotent processing)
- Log failures with traceId and tenantId

Never log and throw repeatedly across layers — log once at the appropriate level.

---

# Validation Standards

## Philosophy

Validation is multi-layered and must occur at:
1. API boundaries (Jakarta Validation)
2. Application workflows (business rule checks)
3. Domain invariants (protected by domain methods)
4. Persistence constraints (database-enforced)
5. Distributed integration boundaries (event schema validation)

Never rely on frontend validation only or database constraints only.

---

## API Validation

```java
public record CreateAccountRequest(
    @NotBlank String accountName,
    @NotNull UUID tenantId,
    @Email @NotBlank String contactEmail,
    @NotNull @Positive BigDecimal initialBalance,
    @NotBlank @Size(min = 3, max = 3) String currencyCode
) {}

// Controller
@PostMapping
public ResponseEntity<ApiResponse<AccountResponse>> create(
        @Valid @RequestBody CreateAccountRequest request) {
    // @Valid triggers Jakarta Validation; MethodArgumentNotValidException on failure
}
```

---

## Financial Validation

Never use `float` or `double` for money:

```java
// ✅ Correct
BigDecimal amount = request.amount();
if (amount.compareTo(BigDecimal.ZERO) <= 0) {
    throw new InvalidAmountException("Amount must be positive: " + amount);
}

// ❌ Wrong
double amount = request.getAmount(); // precision loss
```

All financial validation: positive amounts, balance consistency, debit == credit, currency compatibility.

---

## Multi-Tenant Validation

Every tenant-aware operation must validate tenant ownership before acting:

```java
Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));

if (!account.getTenantId().equals(tenantContext.getTenantId())) {
    throw new AccountNotFoundException(accountId); // 404, not 403 — avoid tenant leakage
}
```

---

## Event / Async Validation

Incoming events must validate schema compatibility, tenant ownership, business correctness, and idempotency. Invalid events must fail safely and be observable — never silently corrupt workflows.

---

# Logging Standards

## Philosophy

Logs are operational assets. They must provide actionable operational value and support distributed debugging.

Logs should answer: what happened, when, where, why it failed, which tenant/user was impacted.

---

## Framework

Use SLF4J + Logback with structured JSON logging.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
}
```

Never use `System.out.println`. Never use inconsistent logging frameworks.

---

## Log Levels

| Level | When to use | Examples |
|---|---|---|
| ERROR | Failed operations, unexpected exceptions, infrastructure failures | DB connection failure, payment processing failure |
| WARN | Recoverable issues, fallback activation | Retry triggered, degraded external service |
| INFO | Important business operations, lifecycle events | Account created, tenant provisioned |
| DEBUG | Development diagnostics, workflow progression | Query details, intermediate state |
| TRACE | Deep troubleshooting only | Framework-level diagnostics |

Avoid excessive DEBUG in production. TRACE should rarely be enabled.

---

## Structured Log Fields

Every log record must include:

```json
{
  "timestamp": "2026-05-23T12:00:00Z",
  "level": "INFO",
  "service": "j4mb-ledger",
  "traceId": "abc123",
  "correlationId": "req-456",
  "tenantId": "tenant-acme",
  "userId": "user-789",
  "thread": "http-nio-8080",
  "logger": "com.j4mb.ledger.account.AccountService",
  "message": "Account created successfully"
}
```

Use MDC to propagate `traceId`, `correlationId`, and `tenantId` automatically across all log statements in a request.

---

## Security Logging

Log: login attempts, failed authentication, permission denials, token validation failures, tenant violations.

Never log: passwords, JWT tokens, secrets, API keys, sensitive personal data (PII).

---

## Financial Logging

Financial operations require audit-grade logging:

```java
log.info("Journal entry posted: traceId={}, tenantId={}, entryId={}, debit={}, credit={}, currencyCode={}",
    MDC.get("traceId"), tenantContext.getTenantId(), entryId, debit, credit, currencyCode);
```

---

## API Request Logging

Log: incoming request method+path, response status, execution duration, trace ID.

Avoid logging: full request bodies containing sensitive data, credentials, PII.

---

## Performance Logging

Log slow operations with threshold-based alerting. Threshold: requests exceeding 2 seconds.

```java
long startMs = System.currentTimeMillis();
// ... operation ...
long durationMs = System.currentTimeMillis() - startMs;
if (durationMs > 2000) {
    log.warn("Slow operation: path={}, durationMs={}, traceId={}",
        path, durationMs, MDC.get("traceId"));
}
```

---

## Kafka Logging

Kafka workflows must log: publish failures, consumer retries, dead-letter events, message processing failures.

Async processing must preserve: trace IDs, tenant context, correlation IDs across all log statements.

---

## Observability Integration

Logging integrates with:
- OpenTelemetry (distributed tracing)
- Prometheus (metrics)
- Grafana (dashboards)
- ELK Stack / Loki / OpenSearch (log aggregation)

---

# AI Code Generation Rules

AI-generated code must:
- Use `@RestControllerAdvice` for centralized exception handling
- Generate structured `ApiResponse<T>` error responses with `traceId`
- Use Jakarta Validation on all request DTOs
- Use SLF4J structured logging with MDC context
- Include operational context in all log messages

Do not generate:
- Raw stack trace exposure in API responses
- Inconsistent error formats
- `catch(Exception e) {}` (swallowed exceptions)
- `System.out.println`
- Logging of passwords, tokens, or secrets
- Excessive DEBUG logging in production code
