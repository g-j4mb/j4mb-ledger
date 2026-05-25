---
name: engineering-principles
description: "Load first for any new service or feature. Sets the foundational engineering philosophy for all J4MB backend services: simplicity, explicitness, cloud-native design, and AI-generation guardrails. Trigger on: new project setup, architecture decisions, or any task where the right approach is unclear."
---

# Engineering Principles

## Objective

Define the foundational engineering principles used across all J4MB platform services.

Applies to: backend services, APIs, databases, infrastructure, distributed systems, AI-generated code.

---

# Stack

| Layer | Choice |
|---|---|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.x (latest stable) |
| Build | Maven + Maven Wrapper (`mvnw`) |
| Database | PostgreSQL 16+ |
| Schema management | Flyway (versioned migrations only) |
| Messaging | Apache Kafka |
| Identity | Keycloak (OAuth2 + JWT) |
| Cache | Redis |
| Containers | Docker + Kubernetes |
| Frontend | React (separate service) |

No Lombok. No Gradle. No ddl-auto in production.

---

# Core Philosophy

Systems must prioritize in this order:

1. Simplicity
2. Maintainability
3. Correctness
4. Security
5. Scalability
6. Observability
7. Explicitness
8. Reproducibility

Avoid accidental complexity.

---

# Architecture Philosophy

Prefer:
- Modular systems with domain-oriented design
- Clean separation of concerns with explicit boundaries
- Composable services with stable contracts

Avoid:
- Tightly coupled modules
- Hidden dependencies
- Shared mutable state
- Framework-driven architecture

Services start as modular monoliths with microservice-ready boundaries. They evolve toward event-driven microservices without requiring rewrites.

---

# Cloud-Native Principles

Services must be:
- Stateless where possible
- Horizontally scalable
- Container-friendly (Docker + Kubernetes)
- Observable out of the box
- Automation-ready

---

# Engineering Standards

Generated systems must:
- Compile and run locally without manual steps
- Support reproducible environments via Docker Compose
- Follow deterministic configuration (no hidden runtime behavior)
- Use Flyway for all schema changes (never `ddl-auto=create` or `ddl-auto=update` in production)

---

# Design Principles

Apply:
- SOLID principles
- Clean architecture (dependency inversion, domain isolation)
- Composition over inheritance
- Immutability where practical

Avoid:
- God objects or massive service classes
- Deep inheritance hierarchies
- Static shared state
- Business logic inside controllers

---

# Dependency Principles

Prefer:
- Stable, mature libraries on latest stable LTS versions
- Minimal dependency footprint

Avoid:
- Experimental or abandoned frameworks
- Unnecessary transitive dependencies

---

# API Philosophy

APIs are long-term contracts. They must be:
- Versioned from day one (`/api/v1/...`)
- Documented with OpenAPI/Swagger
- Predictable and contract-oriented
- Stateless

All APIs must provide structured responses, standardized errors, validation, and traceability.

---

# Persistence Principles

Databases must prioritize consistency, auditability, and integrity.

All schema changes must use versioned Flyway migrations. Never use implicit schema generation in production.

---

# Security Principles

Security must be enabled by default, centralized, and explicit. Never hardcode secrets, expose sensitive information, trust client-supplied validation, or log credentials.

Zero-trust: validate identity, permissions, and tenant ownership on every request.

---

# Observability Principles

All services must support:
- Structured JSON logging (SLF4J + Logback)
- Metrics (Micrometer + Prometheus)
- Distributed tracing (OpenTelemetry)
- Health monitoring (Spring Boot Actuator)

Logs must include: timestamp, level, service name, traceId, correlationId, tenantId.

---

# Startup Banner (Required)

Every generated service must print a `StartupInfoListener` banner at `ApplicationReadyEvent` showing:
- Local and external URLs
- Active profiles
- API and Actuator endpoints
- Frontend dev server URL (if `frontend/` directory exists)

This prevents the most common developer confusion: connecting to the wrong port.

---

# Delivery Principles

Prefer incremental delivery, working software, and iterative improvement.

Avoid premature optimization, speculative abstraction, and unnecessary distributed complexity.

---

# AI Code Generation Rules

AI-generated code must:
- Be production-oriented with no placeholder `TODO` methods
- Follow architectural boundaries
- Remain readable and maintainable
- Avoid overengineering and speculative abstractions
- Optimize for clarity and correctness — not cleverness

Do not generate:
- Massive base abstractions
- Unnecessary inheritance
- Fake or stub implementations
- Unused generic utilities
- `ddl-auto=create` or `ddl-auto=update`
- Lombok annotations
