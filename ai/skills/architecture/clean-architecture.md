---
name: clean-architecture
description: "Load when: creating any new service, module, package structure, or reviewing layer violations. Defines the folder layout, dependency rules, DTO separation, and domain isolation for all J4MB backend services."
---

# Clean Architecture Standards

## Objective

Define the architectural structure and dependency rules for all backend services.

Architecture must prioritize: maintainability, modularity, testability, scalability, domain isolation.

---

# Architectural Style

Services follow:
- Clean Architecture principles
- Domain-oriented modular design
- Explicit dependency direction

Preferred structure:
- Modular monolith initially
- Microservice-ready bounded context boundaries
- Domain-first package organization

---

# Dependency Rule

Dependencies always point inward.

Outer layers may depend on inner layers. Inner layers must NEVER depend on:
- Frameworks
- Infrastructure
- Controllers
- Databases
- External services

Business logic must remain framework-independent.

---

# Layer Definitions

## Domain Layer

Contains: business rules, domain entities, value objects, business invariants, domain services.

Must:
- Contain no framework annotations when possible
- Avoid infrastructure dependencies
- Remain pure business logic

Avoid: database logic, HTTP concerns, serialization concerns.

---

## Application Layer

Responsible for: use cases, orchestration, transaction coordination, validation orchestration.

Contains: application services, commands, queries, DTO coordination.

The application layer:
- Coordinates workflows
- Invokes domain logic
- Handles `@Transactional` boundaries

Avoid: persistence implementation details, controller logic.

---

## Infrastructure Layer

Responsible for: persistence, messaging, security, external integrations, framework configuration.

Contains: JPA repositories, Kafka producers, REST clients, security adapters, database configuration.

Infrastructure is replaceable. The domain should not know implementation details.

---

## Interface Layer

Responsible for: REST APIs, request mapping, response formatting, validation entry points.

Contains: controllers, request DTOs, response DTOs, API documentation.

Controllers must remain thin. They should:
- Validate requests
- Delegate to application services
- Return standardized responses

Avoid: business logic, persistence logic, transaction management.

---

# Package Organization

Use domain-oriented packaging. Never use global technical packaging.

**Preferred:**
```
com.j4mb.platform
├── account
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── interfaces
├── payment
├── notification
└── common
```

**Avoid:**
```
com.j4mb.platform
├── controller/
├── service/
└── repository/
```

This technical structure does not scale.

---

# Domain Design Principles

Domains must:
- Encapsulate business rules and own their invariants
- Expose clear boundaries via interfaces

Prefer:
- Rich domain models with explicit business methods
- Meaningful naming rooted in business language

Avoid:
- Anemic entities (pure getters/setters with no behavior)
- Procedural domain logic scattered across services
- Generic utility-driven design

---

# Transaction Boundaries

Transactions must:
- Be managed in the application layer via `@Transactional`
- Remain explicit and protect business consistency

Avoid:
- Transaction management in controllers
- Transaction leakage across domain boundaries

---

# DTO Rules

Always separate:
- Domain entities
- Request DTOs (`CreateAccountRequest`)
- Response DTOs (`AccountResponse`)

Never expose:
- JPA entities directly in API responses
- Internal persistence structures in interfaces

---

# Mapping Rules

Preferred: MapStruct.

Mapping responsibilities:
- DTO ↔ domain
- Domain ↔ persistence
- External ↔ internal models

Avoid excessive manual mapping duplication or leaking infrastructure models into domain logic.

---

# Validation Rules

Validation occurs at multiple levels.

**Interface validation** — use Jakarta Validation (`@NotNull`, `@NotBlank`, `@Email`).

**Domain validation** — business invariants belong inside the domain:
- Balance cannot become negative
- Journal entries must balance
- Tenant ownership must remain consistent

---

# Exception Handling

Business exceptions belong in domain or application layers.

Infrastructure exceptions must not leak directly to APIs. Use centralized `@RestControllerAdvice`.

---

# Infrastructure Isolation

Infrastructure concerns must remain isolated:
- Database technology
- Kafka, Redis
- Keycloak
- External APIs

The domain must not know implementation details.

---

# Framework Isolation

Business logic should remain minimally coupled to Spring Boot, JPA, and messaging frameworks.

This improves testability, maintainability, and portability.

---

# Modularity Principles

Modules should:
- Communicate through explicit contracts
- Minimize coupling and maximize cohesion

Prefer:
- Small focused modules with explicit interfaces
- Bounded contexts matching business domains

Avoid:
- Circular dependencies
- Shared mutable modules
- Hidden cross-domain access

---

# AI Code Generation Rules

When generating architecture:
- Prioritize readability and explicit boundaries
- Maintain dependency direction (outer → inner)
- Avoid unnecessary abstraction

Do not generate:
- Massive generic base classes
- Overuse of inheritance
- Speculative abstractions
- Logic collapsed into service classes
- `@Autowired` field injection (use constructor injection only)
