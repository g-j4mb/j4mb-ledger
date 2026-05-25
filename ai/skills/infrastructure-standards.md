---
name: infrastructure-standards
description: "Load when: writing Dockerfiles, docker-compose files, CI/CD pipelines, project dotfiles (.gitignore, .env.sample, .editorconfig, .gitattributes, .dockerignore), or configuring observability. Covers the complete operational footprint of a J4MB service."
---

# Infrastructure Standards

## Objective

Define enterprise-grade infrastructure, Docker, CI/CD, dotfile, and observability standards for all J4MB platform services.

---

# Project Dotfiles (Required)

Every repository must include these files at the root:

## `.gitignore`

```gitignore
# Maven / Java
/target/
!.mvn/wrapper/maven-wrapper.jar
.mvn/timing.properties

# IDEs
*.iml
.idea/
.vscode/
*.classpath
*.project
*.settings/

# OS
.DS_Store
Thumbs.db

# Logs
logs/
*.log

# Env files — always gitignore .env, always commit .env.sample
.env
.env.*
.env.local
!.env.sample

# Node / frontend
frontend/node_modules/
frontend/dist/
frontend/.vite/
frontend/.eslintcache

# Testcontainers / Docker
.testcontainers.properties
**/.testcontainers/*
.docker/

# Coverage / reports
coverage/
jacoco.exec
surefire-reports/
failsafe-reports/

# Native builds
target/*-exec

# JDTLS workspace
.jdtls-workspace/
```

## `.env.sample` (Commit — Contains Placeholders Only)

```dotenv
# Spring Boot
SPRING_PROFILES_ACTIVE=local
SPRING_BOOT_PORT=8080

# Database
POSTGRES_PORT=5432
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:${POSTGRES_PORT}/mydb
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=change-me

# Git worktree isolation
COMPOSE_PROJECT_NAME=j4mb-ledger

# Keycloak
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/j4mb
KEYCLOAK_CLIENT_ID=j4mb-ledger
KEYCLOAK_CLIENT_SECRET=change-me

# Secrets (never commit real values)
JWT_SECRET=change-me-64-chars-base64-encoded
```

⚠️ AI agents must NEVER read, print, or expose `.env` contents. Only `.env.sample` (placeholder values) may be shown.

## `.editorconfig`

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 4
trim_trailing_whitespace = true

[{*.yml,*.yaml}]
indent_size = 2

[{*.json,*.js,*.ts,*.tsx,*.jsx,*.mjs}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

## `.gitattributes`

```gitattributes
* text=auto eol=lf
*.png binary
*.jpg binary
*.jar binary
*.gz binary
*.zip binary
*.java diff=java
*.sql diff=sql
*.properties diff=java
```

## `.dockerignore`

```dockerignore
.git
.gitignore
target/
node_modules
**/node_modules
frontend/dist
frontend/.vite
.env
.env.*
.idea
.vscode
*.log
coverage
.jdtls-workspace
```

---

# Docker Standards

## Standard `Dockerfile` (JVM)

Multi-stage build with Eclipse Temurin 21, non-root user, health check:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
```

## `compose.yaml` (Dev — Managed by `spring-boot-docker-compose`)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "user"]
      interval: 10s
      timeout: 5s
      retries: 5
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Do not hardcode `container_name`. Use `COMPOSE_PROJECT_NAME` for worktree-safe isolation.

## `docker-compose.yml` (Full Stack — Integration Testing / Staging)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "user"]
      interval: 10s
      timeout: 5s
      retries: 5
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mydb
      SPRING_DATASOURCE_USERNAME: user
      SPRING_DATASOURCE_PASSWORD: password
      JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75.0
    ports:
      - "${SPRING_BOOT_PORT:-8080}:8080"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

## Docker Best Practices

- Use multi-stage builds — build in JDK image, run in JRE image
- Use Alpine variants for smaller footprint
- Run as non-root user always
- Pin specific version tags in production (not `latest`)
- Configure health checks using Spring Boot Actuator
- Set JVM container flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
- Scan images: `docker scout cves myapp:latest`

---

# Git Standards

## Branching Strategy

```
main          — production-ready code
develop       — integration branch
feature/*     — new features (feature/account-api)
hotfix/*      — production fixes (hotfix/journal-balance-fix)
release/*     — release candidates
```

## Conventional Commits

```
feat: add account creation endpoint
fix: correct transaction validation logic
refactor: simplify tenant resolution
docs: add architecture overview
test: add account service integration tests
chore: upgrade Spring Boot to 3.4.1
```

Avoid: `update`, `changes`, `fix stuff`, `WIP`.

## Pull Request Standards

PRs must include: clear summary, scope description, testing notes, architecture impact if applicable.

Keep PRs small and focused. Avoid massive unrelated PRs.

## Security Rules

Never commit:
- `.env` or production configuration with real values
- Credentials, certificates, or private keys
- Generated build artifacts (`target/`, `node_modules/`)

---

# Observability Standards

## Required Components

All J4MB services must support:
- Structured JSON logging (SLF4J + Logback)
- Metrics endpoint (Micrometer + Prometheus)
- Distributed tracing (OpenTelemetry)
- Health checks (Spring Boot Actuator)

## Spring Boot Actuator Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

## Kubernetes Probes

Configure in deployment manifests:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 20
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
startupProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  failureThreshold: 30
  periodSeconds: 5
```

## OpenTelemetry Integration

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev, reduce in production
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

Every log line must include `traceId` via MDC propagation.

## Logback Configuration (`logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="local,dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss} [%thread] %-5level %logger{36} traceId=%X{traceId} tenantId=%X{tenantId} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod,staging">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>correlationId</includeMdcKeyName>
                <includeMdcKeyName>tenantId</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

---

# CI/CD Standards

## GitHub Actions Workflow

```yaml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        run: ./mvnw -B clean verify

      - name: Build Docker image
        run: docker build -t j4mb-ledger:${{ github.sha }} .

      - name: Security scan
        run: docker scout cves j4mb-ledger:${{ github.sha }}
```

Pipeline must include:
1. `./mvnw clean verify` — compile, unit tests, integration tests
2. Docker image build
3. Security scan (Docker Scout or Trivy)

---

# Repository Structure Standard

```
j4mb-ledger/
├── src/
│   ├── main/java/com/j4mb/ledger/
│   │   ├── account/              # domain module
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   └── interfaces/
│   │   └── common/
│   └── main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       ├── application-prod.yml
│       └── db/migration/         # Flyway migrations
├── docs/
│   ├── adr/                      # Architecture Decision Records
│   ├── architecture.md
│   └── database-design.md
├── docker/                       # Additional Docker files
├── .github/workflows/ci.yml
├── .gitignore
├── .env.sample
├── .editorconfig
├── .gitattributes
├── .dockerignore
├── compose.yaml                  # dev database
├── docker-compose.yml            # full stack
├── Dockerfile
├── mvnw / mvnw.cmd / .mvn/
├── pom.xml
└── README.md
```

## README Requirements

Every README must contain:
1. Project overview and architecture summary
2. Technology stack
3. Local development setup
4. Running instructions (`./mvnw spring-boot:run`)
5. Environment variables reference (link to `.env.sample`)
6. API documentation (link to Swagger UI)
7. Testing instructions
8. Folder structure

---

# AI Code Generation Rules

AI-generated infrastructure code must:
- Include all required dotfiles (`.gitignore`, `.env.sample`, `.editorconfig`, `.gitattributes`, `.dockerignore`)
- Use multi-stage Docker builds with non-root user
- Use `COMPOSE_PROJECT_NAME` for container naming (no hardcoded `container_name`)
- Configure Actuator health probes
- Include `maven-failsafe-plugin` for integration tests
- Use structured JSON logging in production profiles
- Set `start-class` in `pom.xml`

Do not generate:
- Plain `docker-compose up` instructions without health check dependencies
- Hardcoded container names that conflict across Git worktrees
- `ddl-auto=create` or `ddl-auto=update` in any configuration
- Production configuration with hardcoded secrets
- Single-stage Dockerfiles (no build/run separation)
