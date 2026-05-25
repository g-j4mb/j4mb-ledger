---
name: testing-standards
description: "Load when: writing unit tests, integration tests, Testcontainers configuration, or reviewing test coverage. Covers Spring Boot test patterns, @MockitoBean usage, Testcontainers setup, Given-When-Then structure, and the Maven Failsafe configuration required for integration tests."
---

# Testing Standards

## Objective

Define enterprise-grade testing standards for all J4MB backend services.

Testing ensures: correctness, confidence in deployments, domain invariant enforcement, and regression prevention.

---

# Testing Strategy

| Layer | Tool | Speed | Scope |
|---|---|---|---|
| Unit tests | JUnit 5 + Mockito | Fast | Individual classes in isolation |
| Controller tests | `@WebMvcTest` + `@MockitoBean` | Fast | Controller layer only |
| Integration tests | `@SpringBootTest` + Testcontainers | Slow | Full stack with real DB |
| Architecture tests | ArchUnit | Fast | Structural invariants |

---

# Required Dependencies

```xml
<!-- Core test starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- Architecture tests -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

# Maven Failsafe (Required for Integration Tests)

⚠️ **Critical:** Spring Boot's parent POM declares `maven-failsafe-plugin` under `<pluginManagement>` only. Without explicitly declaring it in `<build><plugins>`, `./mvnw verify` reports **BUILD SUCCESS** while silently skipping all `*IT.java` tests.

Add to `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
</plugin>
```

Verify: `./mvnw verify` should show `failsafe:integration-test` in the log with your IT tests listed.

---

# Test Naming Conventions

| Type | Suffix | Runner | When |
|---|---|---|---|
| Unit test | `Test` | Maven Surefire | Fast, no containers |
| Integration test | `IT` | Maven Failsafe | Full stack with DB |

```
src/test/java/com/j4mb/ledger/
├── TestcontainersConfiguration.java   # package-private
├── AccountIntegrationIT.java          # full-stack test
├── JournalEntryIntegrationIT.java
├── account/
│   ├── AccountServiceTest.java        # unit test
│   └── AccountControllerTest.java     # @WebMvcTest
└── architecture/
    └── ArchitectureTest.java          # ArchUnit
```

Integration tests (`*IT.java`) must live in the **same package** as `TestcontainersConfiguration` (package-private class), typically `com.j4mb.ledger` root.

---

# Testcontainers Configuration

```java
// package-private — no public modifier
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withReuse(true);
    }
}
```

⚠️ `TestcontainersConfiguration` must be **package-private** (no `public` modifier). This is a Spring Boot requirement.

Enable local reuse (add to `~/.testcontainers.properties`):
```properties
testcontainers.reuse.enable=true
```

Document this in your project README — without it, containers restart on every test run.

---

# Unit Tests

Use `@ExtendWith(MockitoExtension.class)` for pure unit tests without a Spring context:

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccount_whenValidRequest() {
        // Given
        UUID tenantId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest("Operating Account", "MYR");
        when(tenantContext.getTenantId()).thenReturn(tenantId);
        Account savedAccount = Account.create("Operating Account", "MYR", tenantId);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // When
        AccountResponse response = accountService.create(request);

        // Then
        assertThat(response.accountName()).isEqualTo("Operating Account");
        assertThat(response.currencyCode()).isEqualTo("MYR");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void shouldThrow_whenAccountNotFound() {
        // Given
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> accountService.findById(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(accountId.toString());
    }
}
```

---

# Controller Tests

```java
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean   // Spring Boot 3.x — use @MockitoBean, not deprecated @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(roles = "ACCOUNT_ADMIN")
    void shouldCreateAccount_andReturn201() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("Operating Account", "MYR");
        AccountResponse response = new AccountResponse(UUID.randomUUID(), "Operating Account", "MYR");
        when(accountService.create(any())).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountName").value("Operating Account"));
    }

    @Test
    void shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ACCOUNT_ADMIN")
    void shouldReturn400_whenRequestInvalid() throws Exception {
        // Given — missing required fields
        String invalidRequest = "{}";

        // When / Then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
```

---

# Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();  // clean state per test
    }

    @Test
    @WithMockUser(roles = "ACCOUNT_ADMIN")
    void shouldCreateAndRetrieveAccount() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("Operating Account", "MYR");

        // When — create
        String createResponse = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID accountId = objectMapper.readTree(createResponse)
                .path("data").path("id").traversalAsUUID();

        // Then — verify persisted
        assertThat(accountRepository.findById(accountId)).isPresent();

        // And — retrieve
        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountName").value("Operating Account"));
    }
}
```

---

# Architecture Tests

Enforce clean architecture boundaries with ArchUnit:

```java
@AnalyzeClasses(packages = "com.j4mb.ledger")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule controllersMustNotContainBusinessLogic =
            noClasses().that().resideInAPackage("..interfaces..")
                    .should().dependOnClassesThat().resideInAPackage("..domain..");

    @ArchTest
    static final ArchRule noLombok =
            noClasses().should().accessClassesThat().haveNameMatching(".*lombok.*")
                    .because("Lombok is not allowed — use records or explicit code");

    @ArchTest
    static final ArchRule servicesMustNotCallRepositoriesDirectly =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
}
```

---

# Financial Domain Tests

Test ledger invariants explicitly:

```java
@Test
void journalEntry_mustBalance() {
    // Given
    BigDecimal debit = new BigDecimal("1500.00");
    BigDecimal credit = new BigDecimal("1000.00");  // unbalanced

    // When / Then
    assertThatThrownBy(() ->
            journalEntryService.post(CreateJournalEntryRequest.of(debit, credit)))
            .isInstanceOf(UnbalancedJournalEntryException.class);
}

@Test
void financialHistory_mustBeImmutable() {
    // Given
    JournalEntry entry = journalEntryRepository.findById(entryId).orElseThrow();

    // When / Then — cannot mutate financial history
    assertThatThrownBy(() -> journalEntryService.update(entryId, newAmount))
            .isInstanceOf(ImmutableFinancialRecordException.class);
}
```

---

# Multi-Tenant Tests

Test tenant isolation explicitly:

```java
@Test
@WithMockUser(roles = "ACCOUNT_ADMIN")
void shouldPreventCrossTenantAccess() throws Exception {
    // Given — account belongs to tenant A
    UUID tenantAId = UUID.randomUUID();
    Account tenantAAccount = createAccountForTenant(tenantAId);

    // When — request made with tenant B context
    mockMvc.perform(get("/api/v1/accounts/" + tenantAAccount.getId())
                    .header("X-Tenant-ID", "tenant-b"))  // tenant B context
            .andExpect(status().isNotFound());  // 404, not 403 — prevent leakage
}
```

---

# Testing Best Practices

**Structure:** Use Given-When-Then consistently. One assertion focus per test. Use `@BeforeEach` for common setup.

**Assertions:** Prefer AssertJ (`assertThat`) over JUnit assertions. Test both success and failure paths. Verify exception types and messages.

**Coverage targets:**
- Domain layer: 90%+
- Application layer: 85%+
- Infrastructure/controller: 70%+

Focus coverage on business logic and domain invariants, not on simple getters or framework code.

**Performance:** Separate unit tests (`./mvnw test`) from integration tests (`./mvnw verify`). Keep unit tests under 1 second total. Share Testcontainers across tests with `withReuse(true)`.

---

# Running Tests

```bash
# Unit tests only (fast)
./mvnw test

# All tests including integration (requires Docker)
./mvnw verify

# Specific test class
./mvnw test -Dtest=AccountServiceTest

# Specific integration test
./mvnw verify -Dit.test=AccountIntegrationIT

# Skip tests
./mvnw package -DskipTests
```

---

# AI Code Generation Rules

AI-generated tests must:
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Use `@MockitoBean` (not deprecated `@MockBean`) for controller tests
- Follow Given-When-Then pattern
- Test both success paths and failure/edge cases
- Use Testcontainers with `@ServiceConnection` for integration tests
- Place `TestcontainersConfiguration` as package-private
- Activate `maven-failsafe-plugin` for `*IT.java` tests
- Include ArchUnit tests for clean architecture enforcement
- Include financial invariant tests for ledger operations

Do not generate:
- Empty test methods with `TODO` bodies
- Tests without assertions
- `@MockBean` (deprecated — use `@MockitoBean`)
- Public `TestcontainersConfiguration` class
- Missing `maven-failsafe-plugin` declaration
- Tests that share mutable state across test methods
