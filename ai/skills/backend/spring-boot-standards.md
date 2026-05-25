---
name: spring-boot-standards
description: "Load when: bootstrapping a new Spring Boot service, adding dependencies, configuring profiles, or reviewing pom.xml. Defines the complete technology stack, required dependencies, configuration patterns, and production-readiness requirements."
---

# Spring Boot Standards

## Objective

Define enterprise-grade Spring Boot standards for all J4MB backend services.

---

# Version Policy

| Component | Version |
|---|---|
| Java | 21 LTS |
| Spring Boot | 3.x (latest stable release only — no milestones, no RCs) |
| PostgreSQL | 16+ |
| Maven | 3.8+ |

Use Maven Wrapper (`mvnw`). Required files: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/`.

No Lombok. No Gradle. No Kotlin (unless explicitly scoped).

---

# Required `pom.xml` Properties

Always set `start-class` so AOT processing and Docker builds can find the main class:

```xml
<properties>
    <java.version>21</java.version>
    <start-class>com.j4mb.platform.MyServiceApplication</start-class>
</properties>
```

---

# Required Core Dependencies

```xml
<!-- Web -->
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-validation</dependency>

<!-- Data -->
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>postgresql</dependency>
<dependency>flyway-core</dependency>

<!-- Ops -->
<dependency>spring-boot-starter-actuator</dependency>

<!-- Dev only -->
<dependency>spring-boot-devtools (runtime, optional)</dependency>
```

Optional (add when needed):
- `spring-boot-starter-security` — only when auth is required
- `spring-kafka` — event-driven workflows
- `spring-boot-starter-cache` + `spring-boot-starter-data-redis`
- `springdoc-openapi-starter-webmvc-ui` — OpenAPI/Swagger UI
- `spring-boot-docker-compose` — automatic database startup during development

No Lombok. Use Java records and explicit constructors.

---

# Maven Enforcer (Required)

Add to `pom.xml` to enforce Maven version, Java version, and block Lombok:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <requireMavenVersion><version>[3.8,)</version></requireMavenVersion>
                    <requireJavaVersion><version>[21,)</version></requireJavaVersion>
                    <bannedDependencies>
                        <excludes>
                            <exclude>org.projectlombok:lombok</exclude>
                        </excludes>
                    </bannedDependencies>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

# Configuration Standards

Use `application.yml` with profile-based overrides.

```
src/main/resources/
├── application.yml           # defaults
├── application-local.yml     # local dev
├── application-dev.yml
├── application-staging.yml
└── application-prod.yml
```

**Required `application.yml` entries:**

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  application:
    name: j4mb-ledger
  jpa:
    hibernate:
      ddl-auto: validate    # NEVER create or update in production
    open-in-view: false
  flyway:
    enabled: true

server:
  port: ${SPRING_BOOT_PORT:8080}
  shutdown: graceful
  compression:
    enabled: true

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
```

**Never hardcode in `application.yml`:** passwords, tokens, secrets, API keys.

Use environment variables for all sensitive data:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/mydb}
    username: ${SPRING_DATASOURCE_USERNAME:user}
    password: ${SPRING_DATASOURCE_PASSWORD:password}
```

---

# `.env` Pattern

Use `.env` for local development. Always commit `.env.sample` with placeholders — never commit `.env`.

```dotenv
# .env.sample
SPRING_BOOT_PORT=8080
POSTGRES_PORT=5432
COMPOSE_PROJECT_NAME=j4mb-ledger

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:${POSTGRES_PORT}/mydb
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=change-me
```

Add to `.gitignore`:
```
.env
.env.*
!.env.sample
```

---

# Bean Management

Use constructor injection exclusively. Never use field injection or `@Autowired` on fields.

```java
// ✅ Correct
@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
}

// ❌ Wrong
@Autowired
private AccountRepository accountRepository;
```

---

# Controller Standards

Controllers must remain thin:
- Validate requests (Jakarta Validation)
- Delegate all business logic to application services
- Return standardized responses

```java
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accountService.create(request)));
    }
}
```

Controllers must NOT contain: business rules, persistence logic, transaction management.

---

# Docker Standards

Every service must provide:
- `Dockerfile` — JVM-based, multi-stage build with Eclipse Temurin 21
- `compose.yaml` — dev database via `spring-boot-docker-compose`
- `docker-compose.yml` — full stack for integration testing

**`Dockerfile` requirements:**
- Multi-stage build
- Non-root user for security
- Health check via Actuator
- JVM flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`

**`compose.yaml` (dev only, managed by `spring-boot-docker-compose`):**

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
```

No hardcoded `container_name`. Use `COMPOSE_PROJECT_NAME` for per-worktree isolation.

---

# Startup Banner (Required)

Every service must implement `StartupInfoListener` that fires on `ApplicationReadyEvent` and prints:
- Local and external access URLs
- API endpoint base path
- Actuator URL
- Active profiles
- Frontend dev server URL (if `frontend/` directory exists)

This prevents the most common developer confusion when working with non-default ports.

---

# CI/CD Standards

Use GitHub Actions. Pipeline must include:
1. `./mvnw clean verify` — build + all tests
2. Linting / static analysis
3. Docker image build
4. (Optional) SBOM generation with CycloneDX

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./mvnw -B clean verify
```

---

# AI Code Generation Rules

AI-generated Spring Boot code must:
- Follow clean architecture
- Be production-oriented with no placeholder logic
- Avoid deprecated APIs
- Use constructor injection — never `@Autowired` on fields
- Never use `ddl-auto=create` or `ddl-auto=update`
- Never include Lombok
- Set `start-class` in `pom.xml`
- Remain readable and testable

Do not generate:
- Massive base abstractions
- Unnecessary inheritance
- Speculative generic frameworks
