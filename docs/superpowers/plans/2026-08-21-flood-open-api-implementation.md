# Flood Situation and Material Open API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Java 21 modular-monolith Spring Boot service exposing the eight API-Key-protected flood event, regional situation, and material-demand endpoints defined by the approved specifications.

**Architecture:** Use one Maven module organized by business capability (`common`, `security`, `region`, `event`, `situation`, `material`, `audit`). Controllers translate HTTP only, application services own transactions and orchestration, pure domain services implement deterministic rules, and MyBatis-Plus adapters persist to the existing MySQL schema without database foreign keys.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven, Spring MVC/Security/Validation/Actuator, MyBatis-Plus 3.5.17, MySQL 8.4, Flyway, springdoc-openapi 2.8.17, JUnit 5, AssertJ, ArchUnit, Testcontainers MySQL.

**Spec:** `docs/superpowers/specs/2026-08-21-flood-api-implementation-design.md` and `docs/superpowers/specs/2026-08-21-flood-situation-material-api-design.md`

## Global Constraints

- Use Java 21 and a Spring Boot 3.x line; this plan pins Spring Boot to 3.5.16.
- Use Maven and `mybatis-plus-spring-boot3-starter` 3.5.17.
- Use MySQL 8.4, InnoDB, `utf8mb4`, and UTC `DATETIME(3)` persistence.
- Keep one Maven module and package by business domain; do not add Spring Modulith.
- Keep all existing database foreign keys removed; validate logical references in application services.
- Expose exactly eight endpoints under `/openapi/v1`.
- Authenticate with `X-API-Key`; do not add JWT or OAuth2.
- Require `Idempotency-Key` for the six mutation/calculation endpoints.
- Keep historical assessment and calculation snapshots immutable and reproducible.
- Do not add GIS, hydraulic simulation, queues, asynchronous processing, or management APIs.
- Load sample regions, rules, and material standards only in `local` and `test` profiles.
- Never log or persist a complete API Key.
- Preserve unrelated worktree changes, especially the pre-existing deleted `test` file.

---

## File Structure Map

The implementation creates these responsibility-focused units:

```text
pom.xml
.mvn/wrapper/maven-wrapper.properties
mvnw
mvnw.cmd
sql/mysql/V3__scope_idempotency_by_operation.sql
sql/mysql-local/R__local_reference_data.sql
src/main/java/com/example/flood/FloodApplication.java
src/main/java/com/example/flood/common/api/*
src/main/java/com/example/flood/common/idempotency/*
src/main/java/com/example/flood/common/persistence/*
src/main/java/com/example/flood/common/time/*
src/main/java/com/example/flood/security/api/*
src/main/java/com/example/flood/security/application/*
src/main/java/com/example/flood/security/infrastructure/*
src/main/java/com/example/flood/region/application/*
src/main/java/com/example/flood/region/domain/*
src/main/java/com/example/flood/region/infrastructure/*
src/main/java/com/example/flood/event/api/*
src/main/java/com/example/flood/event/application/*
src/main/java/com/example/flood/event/domain/*
src/main/java/com/example/flood/event/infrastructure/*
src/main/java/com/example/flood/situation/api/*
src/main/java/com/example/flood/situation/application/*
src/main/java/com/example/flood/situation/domain/*
src/main/java/com/example/flood/situation/infrastructure/*
src/main/java/com/example/flood/material/api/*
src/main/java/com/example/flood/material/application/*
src/main/java/com/example/flood/material/domain/*
src/main/java/com/example/flood/material/infrastructure/*
src/main/java/com/example/flood/audit/api/*
src/main/java/com/example/flood/audit/application/*
src/main/java/com/example/flood/audit/infrastructure/*
src/main/resources/application*.yml
src/main/resources/mapper/{event,situation,material}/*.xml
src/test/java/com/example/flood/**/*Test.java
src/test/java/com/example/flood/**/*IT.java
src/test/resources/application-test.yml
README.md
```

`*Row` classes mirror database rows and stay inside infrastructure packages. Domain records do not carry MyBatis annotations. API request/response records stay inside each module's `api` package.

---

### Task 1: Bootstrap the Application and Migration Baseline

**Files:**
- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `src/main/java/com/example/flood/FloodApplication.java`
- Create: `src/main/java/com/example/flood/common/persistence/MybatisConfiguration.java`
- Create: `src/main/java/com/example/flood/common/time/TimeConfiguration.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `src/main/resources/application-prod.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `sql/mysql/V3__scope_idempotency_by_operation.sql`
- Create: `sql/mysql-local/R__local_reference_data.sql`
- Create: `src/test/java/com/example/flood/ApplicationContextTest.java`
- Create: `src/test/java/com/example/flood/architecture/ModuleBoundaryTest.java`

**Interfaces:**
- Consumes: Existing `sql/mysql/V1__create_flood_scenario_schema.sql` and `V2__remove_all_foreign_keys.sql`.
- Produces: A bootable Spring context, UTC `Clock`, MyBatis mapper scanning, Flyway V1-V3 locations, and package dependency rules used by every later task.

- [ ] **Step 1: Add the Maven build and failing context test**

Create a Maven parent using Spring Boot 3.5.16, `java.version` 21, MyBatis-Plus 3.5.17, and springdoc 2.8.17. Include Web, Validation, Security, JDBC, Actuator, Flyway Core/MySQL, MySQL Connector, MyBatis-Plus, springdoc UI, configuration processor, Boot Test, Security Test, ArchUnit JUnit 5, Testcontainers JUnit/MySQL, Surefire, and a Failsafe `integration` profile matching `**/*IT.java`.

After saving `pom.xml`, generate the checked-in Maven Wrapper with:

```powershell
mvn -N wrapper:wrapper -Dmaven=3.9.11
```

Configure the existing `sql` directory as a Maven resource:

```xml
<resources>
  <resource>
    <directory>src/main/resources</directory>
  </resource>
  <resource>
    <directory>sql</directory>
    <targetPath>sql</targetPath>
  </resource>
</resources>
```

Write the context test:

```java
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "flood.security.bootstrap.enabled=false",
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
class ApplicationContextTest {
    @Test void contextLoads() {}
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\mvnw.cmd -Dtest=ApplicationContextTest test`

Expected: FAIL because `FloodApplication` and configuration do not exist yet.

- [ ] **Step 3: Add the minimal application, UTC clock, MyBatis and profile configuration**

Use these exact entry points:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class FloodApplication {
    public static void main(String[] args) {
        SpringApplication.run(FloodApplication.class, args);
    }
}
```

Annotate every mapper interface created by later tasks with `org.apache.ibatis.annotations.Mapper`; MyBatis-Plus will discover them without a broad package scanner.

```java
@Configuration
public class TimeConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
}
```

Configure `application.yml` with UTC JDBC session initialization, `classpath:sql/mysql` Flyway location, `/actuator/health`, snake-case-disabled Jackson defaults, and MyBatis underscore-to-camel mapping. `application-local.yml` adds `classpath:sql/mysql-local` and sets `baseline-on-migrate: true` with `baseline-version: 2`, allowing the already-created local V1/V2 schema to adopt Flyway history before V3. `application-prod.yml` excludes local seed data and does not baseline implicitly; production operators must either start from an empty schema or explicitly approve a one-time baseline at version 2.

Create V3 with this migration sequence:

```sql
USE flood_scenario_deduction;

ALTER TABLE idempotency_record
  ADD COLUMN operation_code VARCHAR(128) NULL AFTER client_id;

UPDATE idempotency_record
SET operation_code = CONCAT('LEGACY:', resource_type)
WHERE operation_code IS NULL;

ALTER TABLE idempotency_record
  MODIFY operation_code VARCHAR(128) NOT NULL,
  DROP INDEX uk_idempotency_client_key,
  ADD UNIQUE KEY uk_idempotency_client_operation_key
    (client_id, operation_code, idempotency_key);
```

Seed local/test data with a province/city/district chain, one ACTIVE `HIGHEST_EVENT_LEVEL` rule set, GTE rules for water depth and trapped population, one ACTIVE material standard set, and regional plus global standards for `DRINKING_WATER`, `INSTANT_FOOD`, `TENT`, and `MEDICAL_KIT`. Use deterministic codes and versions, and use MySQL `ON DUPLICATE KEY UPDATE` clauses so profile restarts are safe.

- [ ] **Step 4: Add and run the module-boundary test**

```java
@AnalyzeClasses(packages = "com.example.flood")
class ModuleBoundaryTest {
    @ArchTest
    static final ArchRule api_does_not_access_persistence =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_stays_independent =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule modules_have_no_cycles =
        SlicesRuleDefinition.slices()
            .matching("com.example.flood.(*)..")
            .should().beFreeOfCycles();
}
```

Run: `.\mvnw.cmd test`

Expected: PASS for context and architecture tests.

- [ ] **Step 5: Commit the bootstrap**

```bash
git add pom.xml .mvn mvnw mvnw.cmd src/main src/test sql/mysql/V3__scope_idempotency_by_operation.sql sql/mysql-local/R__local_reference_data.sql
git commit -m "feat: bootstrap flood open api service"
```

Do not stage the unrelated `test` deletion.

---

### Task 2: Common HTTP Contract, Validation, IDs, and Request Tracing

**Files:**
- Create: `src/main/java/com/example/flood/common/api/ErrorCode.java`
- Create: `src/main/java/com/example/flood/common/api/ApiException.java`
- Create: `src/main/java/com/example/flood/common/api/ApiErrorResponse.java`
- Create: `src/main/java/com/example/flood/common/api/ApiFieldError.java`
- Create: `src/main/java/com/example/flood/common/api/GlobalExceptionHandler.java`
- Create: `src/main/java/com/example/flood/common/api/PageResponse.java`
- Create: `src/main/java/com/example/flood/common/api/PublicIdGenerator.java`
- Create: `src/main/java/com/example/flood/common/api/RequestIdFilter.java`
- Create: `src/main/java/com/example/flood/common/api/RequestContext.java`
- Create: `src/main/java/com/example/flood/common/api/RequestContextHolder.java`
- Test: `src/test/java/com/example/flood/common/api/GlobalExceptionHandlerTest.java`
- Test: `src/test/java/com/example/flood/common/api/RequestIdFilterTest.java`

**Interfaces:**
- Consumes: UTC `Clock` from Task 1.
- Produces: `ApiException(ErrorCode, String, List<ApiFieldError>)`, `PageResponse<T>`, `PublicIdGenerator.next(String prefix)`, and per-request `RequestContext(requestId, clientId, apiKeyId, clientCode, scopes, remoteIp)`.

- [ ] **Step 1: Write failing error-envelope and request-ID tests**

Assert malformed JSON maps to `MALFORMED_REQUEST`, validation failures map to `VALIDATION_ERROR`, `new ApiException(ErrorCode.REGION_NOT_FOUND, "region not found", List.of())` maps to 404, and an absent request ID is generated and returned in both the header and holder.

```java
assertThat(response.errorCode()).isEqualTo("REGION_NOT_FOUND");
assertThat(response.requestId()).startsWith("req_");
assertThat(result.getResponse().getHeader("X-Request-Id"))
    .isEqualTo(requestIdFromBody);
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=GlobalExceptionHandlerTest,RequestIdFilterTest test`

Expected: FAIL because the response envelope and filter do not exist.

- [ ] **Step 3: Implement the exact common contracts**

Define all approved errors in `ErrorCode`, each with an HTTP status:

```java
public enum ErrorCode {
    MALFORMED_REQUEST(400), INVALID_API_KEY(401), INSUFFICIENT_SCOPE(403),
    REGION_NOT_FOUND(404), EVENT_NOT_FOUND(404),
    REGION_NAME_AMBIGUOUS(409), REGION_SELECTOR_MISMATCH(409),
    EVENT_CONFLICT(409), OBSERVATION_CONFLICT(409), IDEMPOTENCY_CONFLICT(409),
    VALIDATION_ERROR(422), INVALID_TIME_RANGE(422), REGION_EVENT_MISMATCH(422),
    NO_ACTIVE_RULE_SET(422), NO_SITUATION_DATA(422),
    NO_ACTIVE_MATERIAL_STANDARD(422), NO_MATERIAL_STANDARD_ITEM(422),
    UNIT_MISMATCH(422), RATE_LIMIT_EXCEEDED(429), INTERNAL_ERROR(500);
}
```

Use records:

```java
public record ApiErrorResponse(
    String requestId,
    String errorCode,
    String message,
    List<ApiFieldError> details,
    OffsetDateTime timestamp) {}

public record PageResponse<T>(
    List<T> items, int page, int size, long totalElements, int totalPages) {}
```

`RequestIdFilter` accepts `[A-Za-z0-9._:-]{1,64}`; otherwise it generates `req_` plus a lowercase ULID-like sortable identifier. Clear the thread-local holder in `finally`.

- [ ] **Step 4: Run common tests**

Run: `.\mvnw.cmd -Dtest='com.example.flood.common.api.*Test' test`

Expected: PASS.

- [ ] **Step 5: Commit common HTTP support**

```bash
git add src/main/java/com/example/flood/common src/test/java/com/example/flood/common
git commit -m "feat: add common api contract and request tracing"
```

---

### Task 3: Region Resolution and Parent-Chain Selection

**Files:**
- Create: `src/main/java/com/example/flood/region/domain/RegionStatus.java`
- Create: `src/main/java/com/example/flood/region/domain/ResolvedRegion.java`
- Create: `src/main/java/com/example/flood/region/application/RegionSelector.java`
- Create: `src/main/java/com/example/flood/region/application/RegionLookup.java`
- Create: `src/main/java/com/example/flood/region/application/RegionResolver.java`
- Create: `src/main/java/com/example/flood/region/infrastructure/RegionRow.java`
- Create: `src/main/java/com/example/flood/region/infrastructure/RegionMapper.java`
- Create: `src/main/java/com/example/flood/region/infrastructure/MybatisRegionLookup.java`
- Test: `src/test/java/com/example/flood/region/application/RegionResolverTest.java`

**Interfaces:**
- Consumes: `ApiException` and `ErrorCode` from Task 2.
- Produces: `ResolvedRegion RegionResolver.resolve(RegionSelector)` and `List<Long> RegionResolver.specificToGlobalScopeIds(ResolvedRegion)` where the list ends with `0L` for global standards.

- [ ] **Step 1: Write failing selector tests**

Cover ID-only, name-only, matching ID+name, mismatched selectors, duplicate names, inactive regions, and a district-to-city-to-province-to-global chain. Include a parent cycle fixture and assert `INTERNAL_ERROR` rather than looping.

```java
assertThatThrownBy(() -> resolver.resolve(new RegionSelector("320111", "Other")))
    .isInstanceOfSatisfying(ApiException.class,
        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.REGION_SELECTOR_MISMATCH));
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\mvnw.cmd -Dtest=RegionResolverTest test`

Expected: FAIL because `RegionResolver` is missing.

- [ ] **Step 3: Implement lookup and deterministic resolution**

Use this port:

```java
public interface RegionLookup {
    Optional<ResolvedRegion> findActiveByCode(String regionCode);
    List<ResolvedRegion> findActiveByNormalizedName(String normalizedName);
    Optional<ResolvedRegion> findActiveByDatabaseId(long id);
}
```

Normalize names with Unicode NFKC and trimmed internal whitespace; do not remove administrative suffixes. Treat API `regionId` as `region.region_code`, never as the database numeric ID. Resolve parent IDs iteratively with a visited-ID set and a maximum depth of 16.

- [ ] **Step 4: Run region and architecture tests**

Run: `.\mvnw.cmd -Dtest=RegionResolverTest,ModuleBoundaryTest test`

Expected: PASS.

- [ ] **Step 5: Commit region resolution**

```bash
git add src/main/java/com/example/flood/region src/test/java/com/example/flood/region
git commit -m "feat: add region selector resolution"
```

---

### Task 4: API Key Authentication, Scope Authorization, Bootstrap, and Rate Limiting

**Files:**
- Create: `src/main/java/com/example/flood/security/application/SecurityProperties.java`
- Create: `src/main/java/com/example/flood/security/application/ApiPrincipal.java`
- Create: `src/main/java/com/example/flood/security/application/ApiKeyCredential.java`
- Create: `src/main/java/com/example/flood/security/application/ApiKeyAuthenticationService.java`
- Create: `src/main/java/com/example/flood/security/application/ApiKeyHasher.java`
- Create: `src/main/java/com/example/flood/security/application/ClientRateLimiter.java`
- Create: `src/main/java/com/example/flood/security/application/BootstrapApiKeyInitializer.java`
- Create: `src/main/java/com/example/flood/security/infrastructure/ApiClientRow.java`
- Create: `src/main/java/com/example/flood/security/infrastructure/ApiKeyRow.java`
- Create: `src/main/java/com/example/flood/security/infrastructure/ApiClientMapper.java`
- Create: `src/main/java/com/example/flood/security/infrastructure/ApiKeyMapper.java`
- Create: `src/main/java/com/example/flood/security/api/ApiKeyAuthenticationFilter.java`
- Create: `src/main/java/com/example/flood/security/api/ScopeAuthorizationManager.java`
- Create: `src/main/java/com/example/flood/security/api/SecurityConfiguration.java`
- Test: `src/test/java/com/example/flood/security/application/ApiKeyHasherTest.java`
- Test: `src/test/java/com/example/flood/security/application/ClientRateLimiterTest.java`
- Test: `src/test/java/com/example/flood/security/api/SecurityConfigurationTest.java`

**Interfaces:**
- Consumes: `RequestContextHolder`, UTC `Clock`, and security tables from V1/V2.
- Produces: authenticated `ApiPrincipal(long clientId, long apiKeyId, String clientCode, Set<String> scopes)`, `ApiKeyAuthenticationService.authenticate(String rawKey, String remoteIp)`, and per-client token-bucket enforcement.

- [ ] **Step 1: Write failing hash, expiry, scope, and refill tests**

Use a fixed pepper and clock. Verify HMAC-SHA256 output is 32 bytes, one-character key changes fail constant-time comparison, expired/revoked keys return `INVALID_API_KEY`, missing scope returns `INSUFFICIENT_SCOPE`, and a 60/minute bucket refills at one token per second.

```java
assertThat(limiter.tryAcquire(10L, 2, Instant.parse("2026-08-21T00:00:00Z"))).isTrue();
assertThat(limiter.tryAcquire(10L, 2, Instant.parse("2026-08-21T00:00:00Z"))).isTrue();
assertThat(limiter.tryAcquire(10L, 2, Instant.parse("2026-08-21T00:00:00Z"))).isFalse();
assertThat(limiter.tryAcquire(10L, 2, Instant.parse("2026-08-21T00:00:30Z"))).isTrue();
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=ApiKeyHasherTest,ClientRateLimiterTest,SecurityConfigurationTest test`

Expected: FAIL because security components are absent.

- [ ] **Step 3: Implement authentication and bootstrap**

Parse keys using:

```java
private static final Pattern KEY_PATTERN = Pattern.compile(
    "^flood_live_([A-Za-z0-9]{8,32})\\.([A-Za-z0-9_-]{32,})$");
```

Compute the digest with `Mac.getInstance("HmacSHA256")` over UTF-8 bytes of the complete key and compare using `MessageDigest.isEqual`. Query by unique `key_prefix`, then validate key/client status, `expires_at`, exact-string IP allowlist entries, and required scope. Update `last_used_at` only when its previous value is null or at least five minutes old, using one conditional SQL update to avoid a write on every request. Do not include the raw header in any exception.

Map operations to scopes explicitly in `ScopeAuthorizationManager`:

```java
POST /openapi/v1/disaster-events                                -> event:write
PUT  /openapi/v1/disaster-events/{eventId}                      -> event:write
POST /openapi/v1/disaster-events/{eventId}/observations         -> event:write
GET  /openapi/v1/disaster-events                                -> event:read
POST /openapi/v1/region-situation-assessments                   -> situation:calculate
GET  /openapi/v1/region-situation-assessments                   -> situation:read
POST /openapi/v1/material-demand-calculations                   -> material:calculate
POST /openapi/v1/material-demand-calculations/from-region-data  -> material:calculate
```

Bootstrap from `FLOOD_BOOTSTRAP_CLIENT_CODE`, `FLOOD_BOOTSTRAP_API_KEY`, and `FLOOD_API_KEY_PEPPER`. Upsert the client and key prefix, store only the digest, set all five scopes, and calculate expiry from `flood.security.bootstrap-key-ttl` defaulting to 365 days. In `prod`, fail startup if bootstrap or pepper configuration is absent.

- [ ] **Step 4: Implement security filter behavior and run tests**

Permit `/actuator/health`, `/v3/api-docs/**`, and `/swagger-ui/**`; protect `/openapi/v1/**`; deny other unmatched application routes. Return the common JSON error envelope for 401, 403, and 429.

Run: `.\mvnw.cmd -Dtest='com.example.flood.security.*Test' test`

Expected: PASS and captured logs contain no complete API Key.

- [ ] **Step 5: Commit security**

```bash
git add src/main/java/com/example/flood/security src/test/java/com/example/flood/security src/main/resources/application*.yml
git commit -m "feat: add api key security and rate limiting"
```

---

### Task 5: Operation-Scoped Idempotency Executor

**Files:**
- Create: `src/main/java/com/example/flood/common/idempotency/IdempotencyProperties.java`
- Create: `src/main/java/com/example/flood/common/idempotency/CanonicalRequestHasher.java`
- Create: `src/main/java/com/example/flood/common/idempotency/IdempotentOperation.java`
- Create: `src/main/java/com/example/flood/common/idempotency/OperationResult.java`
- Create: `src/main/java/com/example/flood/common/idempotency/IdempotentResult.java`
- Create: `src/main/java/com/example/flood/common/idempotency/IdempotencyExecutor.java`
- Create: `src/main/java/com/example/flood/common/idempotency/IdempotencyRecordRow.java`
- Create: `src/main/java/com/example/flood/common/idempotency/IdempotencyRecordMapper.java`
- Create: `src/main/java/com/example/flood/common/idempotency/ExpiredIdempotencyCleanup.java`
- Test: `src/test/java/com/example/flood/common/idempotency/CanonicalRequestHasherTest.java`
- Test: `src/test/java/com/example/flood/common/idempotency/IdempotencyExecutorTest.java`

**Interfaces:**
- Consumes: authenticated `ApiPrincipal`, V3 `operation_code`, Jackson `ObjectMapper`, `TransactionTemplate`, and UTC `Clock`.
- Produces: `<T> IdempotentResult<T> IdempotencyExecutor.execute(ApiPrincipal, IdempotentOperation, String key, Object requestFingerprintInput, Class<T>, Supplier<OperationResult<T>>)`.

- [ ] **Step 1: Write failing canonicalization and decision tests**

Verify reordered JSON object keys and numerically equivalent decimal forms hash identically, changed path variables hash differently, same operation/key/body replays stored response, different body raises `IDEMPOTENCY_CONFLICT`, and the same key works for different `operationCode` values.

```java
var create = new IdempotentOperation(
    "POST:/openapi/v1/disaster-events", Map.of(), Map.of());
var update = new IdempotentOperation(
    "PUT:/openapi/v1/disaster-events/{eventId}",
    Map.of("eventId", "EVT-1"), Map.of());
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=CanonicalRequestHasherTest,IdempotencyExecutorTest test`

Expected: FAIL because the executor is missing.

- [ ] **Step 3: Implement deterministic hashing and transactional execution**

Use these records:

```java
public record IdempotentOperation(
    String operationCode,
    Map<String, String> pathVariables,
    Map<String, List<String>> queryParameters) {}

public record OperationResult<T>(
    int httpStatus, T body, String resourceType, String resourcePublicId) {}

public record IdempotentResult<T>(
    int httpStatus, T body, boolean replayed) {}
```

Canonicalize to a Jackson tree, recursively sort object fields, normalize `BigDecimal` with `stripTrailingZeros().toPlainString()`, serialize UTF-8, and SHA-256 the operation plus canonical input. Enforce an `Idempotency-Key` length of 1-128.

Inside one `TransactionTemplate`:

1. insert a pending row with response columns null;
2. on duplicate key, re-read by `(clientId, operationCode, key)`;
3. compare hash with `MessageDigest.isEqual`;
4. replay a completed response or return a retryable 409 when still pending;
5. execute the supplier;
6. serialize and save status/body/resource ID;
7. commit all business writes and the idempotency result together.

Delete expired completed rows in batches of 500; never delete unexpired pending rows.

- [ ] **Step 4: Run idempotency tests**

Run: `.\mvnw.cmd -Dtest='com.example.flood.common.idempotency.*Test' test`

Expected: PASS.

- [ ] **Step 5: Commit idempotency support**

```bash
git add src/main/java/com/example/flood/common/idempotency src/test/java/com/example/flood/common/idempotency sql/mysql/V3__scope_idempotency_by_operation.sql
git commit -m "feat: add operation scoped idempotency"
```

---

### Task 6: Event Creation and Full Update APIs

**Files:**
- Create: `src/main/java/com/example/flood/event/domain/EventType.java`
- Create: `src/main/java/com/example/flood/event/domain/EventStatus.java`
- Create: `src/main/java/com/example/flood/event/domain/DisasterEvent.java`
- Create: `src/main/java/com/example/flood/event/domain/EventObservation.java`
- Create: `src/main/java/com/example/flood/event/domain/EventValidator.java`
- Create: `src/main/java/com/example/flood/event/api/CreateEventRequest.java`
- Create: `src/main/java/com/example/flood/event/api/UpdateEventRequest.java`
- Create: `src/main/java/com/example/flood/event/api/EventObservationRequest.java`
- Create: `src/main/java/com/example/flood/event/api/HazardRequest.java`
- Create: `src/main/java/com/example/flood/event/api/ImpactRequest.java`
- Create: `src/main/java/com/example/flood/event/api/EventResponse.java`
- Create: `src/main/java/com/example/flood/event/api/EventController.java`
- Create: `src/main/java/com/example/flood/event/application/CreateEventCommand.java`
- Create: `src/main/java/com/example/flood/event/application/UpdateEventCommand.java`
- Create: `src/main/java/com/example/flood/event/application/EventApplicationService.java`
- Create: `src/main/java/com/example/flood/event/application/DefaultEventApplicationService.java`
- Create: `src/main/java/com/example/flood/event/infrastructure/DisasterEventRow.java`
- Create: `src/main/java/com/example/flood/event/infrastructure/EventObservationRow.java`
- Create: `src/main/java/com/example/flood/event/infrastructure/DisasterEventMapper.java`
- Create: `src/main/java/com/example/flood/event/infrastructure/EventObservationMapper.java`
- Test: `src/test/java/com/example/flood/event/domain/EventValidatorTest.java`
- Test: `src/test/java/com/example/flood/event/api/EventCreateUpdateControllerTest.java`

**Interfaces:**
- Consumes: `RegionResolver`, `ApiPrincipal`, `IdempotencyExecutor`, `PublicIdGenerator`.
- Produces: `EventResponse EventApplicationService.create(CreateEventCommand, ApiPrincipal)` and `EventResponse update(String eventId, UpdateEventCommand, ApiPrincipal)`.

- [ ] **Step 1: Write failing validation and controller tests**

Cover `ONGOING` with non-null end, `ENDED` without end, end before start, observation before start, negative metrics, impact subgroup greater than affected population, duplicate `(sourceSystem, externalEventId)`, immutable identity fields absent from PUT, create 201, update 200, and idempotent replay.

```java
assertThatThrownBy(() -> validator.validateTimes(
        EventStatus.ENDED, start, null))
    .isInstanceOfSatisfying(ApiException.class,
        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=EventValidatorTest,EventCreateUpdateControllerTest test`

Expected: FAIL because event contracts and service are absent.

- [ ] **Step 3: Implement event validation and persistence**

Use these service signatures:

```java
public interface EventApplicationService {
    EventResponse create(CreateEventCommand command, ApiPrincipal principal);
    EventResponse update(String eventId, UpdateEventCommand command,
                         ApiPrincipal principal);
}
```

On create, resolve the region, reject existing source/external ID, generate `EVT_` and optional `OBS_` public IDs, insert the event and initial observation, and return timestamps from the UTC clock. Convert duplicate-key exceptions to `EVENT_CONFLICT`.

On update, select by public ID `FOR UPDATE`, preserve `public_id`, external ID, source system, region, and creator, validate the five mutable fields, and update only event type/name/start/end/status. Historical assessment rows are untouched.

- [ ] **Step 4: Wire the POST and PUT controllers through idempotency**

Use operation codes `POST:/openapi/v1/disaster-events` and `PUT:/openapi/v1/disaster-events/{eventId}`. Map request records to commands and call:

```java
idempotencyExecutor.execute(
    principal,
    operation,
    idempotencyKey,
    request,
    EventResponse.class,
    () -> {
        EventResponse created = service.create(command, principal);
        return new OperationResult<>(201, created, "EVENT", created.eventId());
    });
```

Use status 200 for PUT. Create and its idempotent replay both return the stored status 201 and the same resource body.

Run: `.\mvnw.cmd -Dtest=EventValidatorTest,EventCreateUpdateControllerTest test`

Expected: PASS.

- [ ] **Step 5: Commit event create/update**

```bash
git add src/main/java/com/example/flood/event src/test/java/com/example/flood/event
git commit -m "feat: add event create and update apis"
```

---

### Task 7: Event Observation Append and Time-Range Query APIs

**Files:**
- Modify: `src/main/java/com/example/flood/event/api/EventController.java`
- Create: `src/main/java/com/example/flood/event/api/ObservationResponse.java`
- Create: `src/main/java/com/example/flood/event/api/EventSummaryResponse.java`
- Create: `src/main/java/com/example/flood/event/application/AppendObservationCommand.java`
- Create: `src/main/java/com/example/flood/event/application/EventQuery.java`
- Create: `src/main/java/com/example/flood/event/application/EventQueryService.java`
- Modify: `src/main/java/com/example/flood/event/application/EventApplicationService.java`
- Modify: `src/main/java/com/example/flood/event/infrastructure/DisasterEventMapper.java`
- Modify: `src/main/java/com/example/flood/event/infrastructure/EventObservationMapper.java`
- Create: `src/main/resources/mapper/event/DisasterEventMapper.xml`
- Test: `src/test/java/com/example/flood/event/application/ObservationConflictTest.java`
- Test: `src/test/java/com/example/flood/event/application/EventQueryServiceTest.java`
- Test: `src/test/java/com/example/flood/event/api/EventObservationQueryControllerTest.java`

**Interfaces:**
- Consumes: Event persistence from Task 6 and `PageResponse<T>`.
- Produces: `ObservationResponse appendObservation(String eventId, AppendObservationCommand, ApiPrincipal)` and `PageResponse<EventSummaryResponse> search(EventQuery)`.

- [ ] **Step 1: Write failing append and query tests**

Cover event not found, observation earlier than event start, same unique key/same body replay, same unique key/different body conflict, interval overlap, status filter, latest observation inside the query interval, no observation returning null, and stable ordering by `start_time DESC, id DESC`.

Use the interval assertion:

```sql
e.start_time < #{endTimeUtc}
AND (e.end_time IS NULL OR e.end_time >= #{startTimeUtc})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=ObservationConflictTest,EventQueryServiceTest,EventObservationQueryControllerTest test`

Expected: FAIL because append and query use cases are missing.

- [ ] **Step 3: Implement observation conflict semantics**

Serialize the normalized observation fields to a canonical comparison object. If `(event_id, external_observation_id)` or `(event_id, observed_at)` exists:

- return the existing observation when every persisted field matches;
- throw `OBSERVATION_CONFLICT` when any field differs.

The outer `IdempotencyExecutor` still protects the HTTP request; observation natural-key semantics protect imports that use another idempotency key.

- [ ] **Step 4: Implement the paginated query and controller mappings**

The observation endpoint uses operation code `POST:/openapi/v1/disaster-events/{eventId}/observations`. Require exactly one resolved region plus `startTime`, `endTime`, `page >= 0`, and `1 <= size <= 100` for the query endpoint. Convert offset times to UTC. Use one count query and one page query; fetch each event's latest in-range observation with a window function or a `MAX(observed_at)` join, avoiding N+1 queries.

Run: `.\mvnw.cmd -Dtest='com.example.flood.event.*Test' test`

Expected: PASS.

- [ ] **Step 5: Commit observation and query APIs**

```bash
git add src/main/java/com/example/flood/event src/main/resources/mapper/event src/test/java/com/example/flood/event
git commit -m "feat: add event observations and queries"
```

---

### Task 8: Pure Regional Situation Rule Engine

**Files:**
- Create: `src/main/java/com/example/flood/situation/domain/SituationLevel.java`
- Create: `src/main/java/com/example/flood/situation/domain/ComparisonDirection.java`
- Create: `src/main/java/com/example/flood/situation/domain/MetricCode.java`
- Create: `src/main/java/com/example/flood/situation/domain/SituationRuleDefinition.java`
- Create: `src/main/java/com/example/flood/situation/domain/EventSituationInput.java`
- Create: `src/main/java/com/example/flood/situation/domain/EventSituationResult.java`
- Create: `src/main/java/com/example/flood/situation/domain/RegionSituationResult.java`
- Create: `src/main/java/com/example/flood/situation/domain/SituationRuleEngine.java`
- Test: `src/test/java/com/example/flood/situation/domain/SituationRuleEngineTest.java`

**Interfaces:**
- Consumes: Event type and normalized observation metrics.
- Produces: `EventSituationResult evaluateEvent(EventSituationInput, List<SituationRuleDefinition>)` and `RegionSituationResult aggregate(List<EventSituationResult>, Integer mediumToHighCount)`.

- [ ] **Step 1: Write failing boundary tests**

Cover GTE just below/equal/above, LTE just above/equal/below, null metrics not matching, event-type-specific plus global rules, HIGH precedence, MEDIUM fallback, no match LOW, empty region LOW, highest-event aggregation, and medium-count escalation.

```java
assertThat(engine.evaluateEvent(inputWithDepth("1.5000"), rules).level())
    .isEqualTo(SituationLevel.HIGH);
assertThat(engine.aggregate(List.of(medium1, medium2), 2).level())
    .isEqualTo(SituationLevel.HIGH);
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\mvnw.cmd -Dtest=SituationRuleEngineTest test`

Expected: FAIL because the pure engine does not exist.

- [ ] **Step 3: Implement metric extraction and deterministic evaluation**

Map only these database-supported metric codes:

```java
RAINFALL_24H_MM
WATER_LEVEL_OVER_WARNING_M
MAX_WATER_DEPTH_M
AFFECTED_AREA_KM2
AFFECTED_POPULATION
TRAPPED_POPULATION
EVACUATED_POPULATION
VULNERABLE_POPULATION
INJURED_POPULATION
MISSING_POPULATION
DEATH_POPULATION
DAMAGED_HOUSEHOLDS
COLLAPSED_HOUSES
ROAD_INTERRUPTIONS
CRITICAL_FACILITIES_AFFECTED
POWER_OUTAGE_HOUSEHOLDS
```

Reject an unknown enabled metric as a configuration error. Compare with `BigDecimal.compareTo`, collect matched rule codes in `(priority DESC, ruleCode ASC)` order, and derive the highest matched level.

- [ ] **Step 4: Run rule-engine tests**

Run: `.\mvnw.cmd -Dtest=SituationRuleEngineTest test`

Expected: PASS without loading Spring.

- [ ] **Step 5: Commit the rule engine**

```bash
git add src/main/java/com/example/flood/situation/domain src/test/java/com/example/flood/situation/domain
git commit -m "feat: add flood situation rule engine"
```

---

### Task 9: Situation Import/Assessment and Historical Query APIs

**Files:**
- Create: `src/main/java/com/example/flood/situation/api/SituationAssessmentRequest.java`
- Create: `src/main/java/com/example/flood/situation/api/SituationEventImportRequest.java`
- Create: `src/main/java/com/example/flood/situation/api/SituationAssessmentResponse.java`
- Create: `src/main/java/com/example/flood/situation/api/SituationAssessmentSummaryResponse.java`
- Create: `src/main/java/com/example/flood/situation/api/SituationController.java`
- Create: `src/main/java/com/example/flood/situation/application/SituationAssessmentCommand.java`
- Create: `src/main/java/com/example/flood/situation/application/SituationAssessmentQuery.java`
- Create: `src/main/java/com/example/flood/situation/application/RegionSituationAssessmentService.java`
- Create: `src/main/java/com/example/flood/situation/application/ActiveSituationRuleProvider.java`
- Create: `src/main/java/com/example/flood/event/application/EventAssessmentImportPort.java`
- Create: `src/main/java/com/example/flood/event/application/AssessmentEventImportCommand.java`
- Create: `src/main/java/com/example/flood/event/application/ImportedAssessmentEvent.java`
- Create: `src/main/java/com/example/flood/situation/infrastructure/SituationRuleSetRow.java`
- Create: `src/main/java/com/example/flood/situation/infrastructure/SituationRuleRow.java`
- Create: `src/main/java/com/example/flood/situation/infrastructure/SituationAssessmentRow.java`
- Create: `src/main/java/com/example/flood/situation/infrastructure/SituationAssessmentEventRow.java`
- Create: `src/main/java/com/example/flood/situation/infrastructure/SituationRuleMapper.java`
- Create: `src/main/java/com/example/flood/situation/infrastructure/SituationAssessmentMapper.java`
- Create: `src/main/resources/mapper/situation/SituationRuleMapper.xml`
- Create: `src/main/resources/mapper/situation/SituationAssessmentMapper.xml`
- Test: `src/test/java/com/example/flood/situation/application/RegionSituationAssessmentServiceTest.java`
- Test: `src/test/java/com/example/flood/situation/api/SituationControllerTest.java`

**Interfaces:**
- Consumes: `EventAssessmentImportPort`, `SituationRuleEngine`, `RegionResolver`, `IdempotencyExecutor`.
- Produces: `SituationAssessmentResponse assess(SituationAssessmentCommand, ApiPrincipal)` and `PageResponse<SituationAssessmentSummaryResponse> search(SituationAssessmentQuery)`.

- [ ] **Step 1: Write failing orchestration tests**

Cover all events matching the root region, stable-field conflict during aggregate import, event create/update counts, duplicate observation counts, only ONGOING events with `observedAt <= assessmentTime`, active rule-set time selection, no active rule set, population sums, overlap warning, medium escalation, immutable rule/input snapshots, and query ordering.

```java
verify(assessmentMapper).insert(argThat(row ->
    row.getSituationLevel().equals("HIGH")
        && row.getSourceType().equals("DIRECT_IMPORT")
        && row.getRuleVersion().equals("flood-situation-v1.0")));
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=RegionSituationAssessmentServiceTest,SituationControllerTest test`

Expected: FAIL because assessment orchestration is absent.

- [ ] **Step 3: Implement the atomic assessment flow**

`EventAssessmentImportPort` must expose:

```java
public interface EventAssessmentImportPort {
    List<ImportedAssessmentEvent> importForAssessment(
        ResolvedRegion rootRegion,
        Instant assessmentTime,
        List<AssessmentEventImportCommand> events,
        ApiPrincipal principal);
}
```

`SituationController` maps `SituationEventImportRequest` to `AssessmentEventImportCommand` before calling the event module, so the event application package has no dependency on situation API DTOs.

For existing events, compare source/external ID, region, event type, and start time; mismatch raises `EVENT_CONFLICT`. Update only name/status/end time. Save observations using Task 7 natural-key semantics. Select one ACTIVE rule set where `effective_from <= assessmentTime` and `effective_to IS NULL OR assessmentTime < effective_to`; zero matches gives `NO_ACTIVE_RULE_SET`, multiple matches is `INTERNAL_ERROR` with an operator log.

Persist the assessment header first, then one evidence row per participating event. Store canonical `input_snapshot`, matched rule codes, rule version, duration hours to scale 3, and `POSSIBLE_POPULATION_OVERLAP` when required.

- [ ] **Step 4: Implement POST/GET controllers and run tests**

POST uses operation code `POST:/openapi/v1/region-situation-assessments` and returns 201. GET requires region plus `[startTime,endTime)`, supports page/size, returns saved rows only, and orders by `assessment_time DESC, id DESC`.

Run: `.\mvnw.cmd -Dtest='com.example.flood.situation.*Test' test`

Expected: PASS.

- [ ] **Step 5: Commit situation APIs**

```bash
git add src/main/java/com/example/flood/situation src/main/java/com/example/flood/event/application src/main/resources/mapper/situation src/test/java/com/example/flood/situation
git commit -m "feat: add regional situation assessment apis"
```

---

### Task 10: Pure Material Demand Calculator and Standard Selection

**Files:**
- Create: `src/main/java/com/example/flood/material/domain/PopulationBasis.java`
- Create: `src/main/java/com/example/flood/material/domain/PopulationSnapshot.java`
- Create: `src/main/java/com/example/flood/material/domain/MaterialStandardDefinition.java`
- Create: `src/main/java/com/example/flood/material/domain/InventoryItem.java`
- Create: `src/main/java/com/example/flood/material/domain/MaterialDemandInput.java`
- Create: `src/main/java/com/example/flood/material/domain/MaterialDemandLine.java`
- Create: `src/main/java/com/example/flood/material/domain/MaterialDemandCalculator.java`
- Create: `src/main/java/com/example/flood/material/application/MaterialStandardLookup.java`
- Create: `src/main/java/com/example/flood/material/application/SelectedMaterialStandards.java`
- Create: `src/main/java/com/example/flood/material/application/MaterialStandardService.java`
- Test: `src/test/java/com/example/flood/material/domain/MaterialDemandCalculatorTest.java`
- Test: `src/test/java/com/example/flood/material/application/MaterialStandardServiceTest.java`

**Interfaces:**
- Consumes: `SituationLevel`, region parent scope IDs, and business standard rows.
- Produces: `List<MaterialDemandLine> MaterialDemandCalculator.calculate(MaterialDemandInput, List<MaterialStandardDefinition>)` and `SelectedMaterialStandards MaterialStandardService.select(ResolvedRegion, SituationLevel, Instant)`.

- [ ] **Step 1: Write failing formula tests**

Cover 1/24/25/72-hour supply-day rounding, affected/trapped/evacuated/vulnerable bases, fixed basis not multiplying supply days, standard reserve ratio, request reserve override, minimum quantity, package rounding, inventory greater than gross yielding zero, duplicate inventory code, negative inventory, and unit mismatch.

```java
assertThat(calculator.supplyDays(new BigDecimal("25"))).isEqualTo(2);
assertThat(line.grossDemand()).isEqualByComparingTo("12000.0000");
assertThat(line.netDemand()).isEqualByComparingTo("0.0000");
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=MaterialDemandCalculatorTest,MaterialStandardServiceTest test`

Expected: FAIL because the calculator and selector are absent.

- [ ] **Step 3: Implement exact BigDecimal formulas**

Use:

```java
int supplyDays = durationHours
    .divide(BigDecimal.valueOf(24), 0, RoundingMode.CEILING)
    .intValueExact();

BigDecimal raw = basis == PopulationBasis.FIXED
    ? fixedBaseQuantity
    : BigDecimal.valueOf(basisPopulation)
        .multiply(perPersonPerDay)
        .multiply(BigDecimal.valueOf(supplyDays));

BigDecimal adjusted = raw
    .multiply(levelFactor)
    .multiply(BigDecimal.ONE.add(effectiveReserveRatio));

BigDecimal gross = adjusted.max(minimumQuantity)
    .divide(packageSize, 0, RoundingMode.CEILING)
    .multiply(packageSize);

BigDecimal net = gross.subtract(currentInventory).max(BigDecimal.ZERO);
```

Persist/output quantities at scale 4. Do not use `double` or `float`.

- [ ] **Step 4: Implement region-to-global standard selection**

Select one ACTIVE standard set by calculation instant. For each material code, choose the first definition found in the region scope sequence `[district, city, province, 0]`; never combine two rows for one material. If the set is absent return `NO_ACTIVE_MATERIAL_STANDARD`; if it has no item for the requested level return `NO_MATERIAL_STANDARD_ITEM`.

Run: `.\mvnw.cmd -Dtest=MaterialDemandCalculatorTest,MaterialStandardServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit material domain logic**

```bash
git add src/main/java/com/example/flood/material/domain src/main/java/com/example/flood/material/application src/test/java/com/example/flood/material
git commit -m "feat: add material demand calculation engine"
```

---

### Task 11: Direct and Database-Backed Material Calculation APIs

**Files:**
- Create: `src/main/java/com/example/flood/material/api/DirectMaterialCalculationRequest.java`
- Create: `src/main/java/com/example/flood/material/api/RegionDataMaterialCalculationRequest.java`
- Create: `src/main/java/com/example/flood/material/api/PopulationRequest.java`
- Create: `src/main/java/com/example/flood/material/api/InventoryRequest.java`
- Create: `src/main/java/com/example/flood/material/api/MaterialCalculationResponse.java`
- Create: `src/main/java/com/example/flood/material/api/MaterialDemandItemResponse.java`
- Create: `src/main/java/com/example/flood/material/api/MaterialController.java`
- Create: `src/main/java/com/example/flood/material/application/DirectMaterialCalculationCommand.java`
- Create: `src/main/java/com/example/flood/material/application/RegionDataMaterialCalculationCommand.java`
- Create: `src/main/java/com/example/flood/material/application/MaterialDemandCalculationService.java`
- Create: `src/main/java/com/example/flood/situation/application/SituationSnapshotLookup.java`
- Create: `src/main/java/com/example/flood/situation/application/SavedSituationSnapshot.java`
- Create: `src/main/java/com/example/flood/material/infrastructure/MaterialStandardSetRow.java`
- Create: `src/main/java/com/example/flood/material/infrastructure/MaterialStandardRow.java`
- Create: `src/main/java/com/example/flood/material/infrastructure/MaterialCalculationRow.java`
- Create: `src/main/java/com/example/flood/material/infrastructure/MaterialDemandItemRow.java`
- Create: `src/main/java/com/example/flood/material/infrastructure/MaterialStandardMapper.java`
- Create: `src/main/java/com/example/flood/material/infrastructure/MaterialCalculationMapper.java`
- Create: `src/main/resources/mapper/material/MaterialStandardMapper.xml`
- Create: `src/main/resources/mapper/material/MaterialCalculationMapper.xml`
- Test: `src/test/java/com/example/flood/material/application/MaterialDemandCalculationServiceTest.java`
- Test: `src/test/java/com/example/flood/material/api/MaterialControllerTest.java`

**Interfaces:**
- Consumes: `MaterialDemandCalculator`, `MaterialStandardService`, `SituationSnapshotLookup`, `RegionResolver`, `IdempotencyExecutor`.
- Produces: `MaterialCalculationResponse calculateDirect(DirectMaterialCalculationCommand, ApiPrincipal)` and `calculateFromRegionData(RegionDataMaterialCalculationCommand, ApiPrincipal)`.

`SituationSnapshotLookup` has the exact contract:

```java
public interface SituationSnapshotLookup {
    Optional<SavedSituationSnapshot> findLatest(
        long regionDatabaseId, Instant startInclusive, Instant endExclusive);
}

public record SavedSituationSnapshot(
    long databaseId,
    String publicId,
    SituationLevel level,
    PopulationSnapshot population,
    Instant assessmentTime) {}
```

- [ ] **Step 1: Write failing service and controller tests**

Cover direct input, optional reserve override range `[0,1]`, database range selecting `assessment_time DESC, id DESC`, no situation data, no active standard, source/assessment consistency, standard version snapshot, one header plus all details, and idempotent response replay.

```java
when(snapshotLookup.findLatest(region.id(), start, end))
    .thenReturn(Optional.of(savedHighAssessment));
assertThat(response.sourceType()).isEqualTo("DATABASE");
assertThat(response.assessmentId()).isEqualTo("RSA_01");
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -Dtest=MaterialDemandCalculationServiceTest,MaterialControllerTest test`

Expected: FAIL because application/persistence APIs are absent.

- [ ] **Step 3: Implement both calculation transactions**

Direct flow:

1. resolve region;
2. validate duration, population, optional reserve ratio, and inventory uniqueness;
3. select active standard and definitions;
4. run pure calculator;
5. insert `material_demand_calculation` with `source_type='DIRECT'` and null assessment;
6. insert every detail and formula snapshot.

Database flow:

1. resolve region and validate `[startTime,endTime)`;
2. select latest saved assessment in the range;
3. copy its level and four population totals;
4. select standards and calculate;
5. insert header with `source_type='DATABASE'` and the assessment ID;
6. insert every detail and formula snapshot.

Both flows execute inside the Task 5 idempotency transaction and generate `MDC_` public IDs.

- [ ] **Step 4: Wire endpoints and run tests**

Use operation codes:

```text
POST:/openapi/v1/material-demand-calculations
POST:/openapi/v1/material-demand-calculations/from-region-data
```

Return 201 for first execution and the stored body for replay. Keep response field names exactly as the base API specification.

Run: `.\mvnw.cmd -Dtest='com.example.flood.material.*Test' test`

Expected: PASS.

- [ ] **Step 5: Commit material APIs**

```bash
git add src/main/java/com/example/flood/material src/main/java/com/example/flood/situation/application/SituationSnapshotLookup.java src/main/resources/mapper/material src/test/java/com/example/flood/material
git commit -m "feat: add material demand calculation apis"
```

---

### Task 12: Audit, OpenAPI, MySQL Integration Tests, and Operator Documentation

**Files:**
- Create: `src/main/java/com/example/flood/audit/application/ApiAuditService.java`
- Create: `src/main/java/com/example/flood/audit/infrastructure/ApiAuditLogRow.java`
- Create: `src/main/java/com/example/flood/audit/infrastructure/ApiAuditLogMapper.java`
- Create: `src/main/java/com/example/flood/audit/api/ApiAuditFilter.java`
- Create: `src/main/java/com/example/flood/common/api/OpenApiConfiguration.java`
- Create: `src/test/java/com/example/flood/support/MySqlIntegrationTestBase.java`
- Create: `src/test/java/com/example/flood/migration/FlywayMigrationIT.java`
- Create: `src/test/java/com/example/flood/security/ApiKeyAuthenticationIT.java`
- Create: `src/test/java/com/example/flood/event/EventApiIT.java`
- Create: `src/test/java/com/example/flood/situation/SituationApiIT.java`
- Create: `src/test/java/com/example/flood/material/MaterialApiIT.java`
- Create: `src/test/java/com/example/flood/idempotency/IdempotencyConcurrencyIT.java`
- Create: `src/test/java/com/example/flood/transaction/AssessmentRollbackIT.java`
- Create: `sql/mysql/tests/assert_application_references.sql`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `.gitignore`
- Create: `.env.example`
- Create: `README.md`

**Interfaces:**
- Consumes: all eight endpoints and every persisted module.
- Produces: independent audit writes, generated OpenAPI security documentation, full MySQL verification, and runnable operator instructions.

- [ ] **Step 1: Write failing audit and integration tests**

Use a shared MySQL 8.4 container:

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class MySqlIntegrationTestBase {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("flood_scenario_deduction")
        .withUsername("root")
        .withPassword("root");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
```

Tests must assert:

- Flyway reaches V3 and all 15 tables still have zero foreign keys;
- all eight endpoints authenticate and enforce scopes;
- event create/update/observation/query contracts;
- assessment import rollback leaves zero partial rows;
- saved history is unchanged after event/rule changes;
- database material calculation selects the latest in-range assessment;
- concurrent same operation/key creates one resource;
- the same key on another operation succeeds;
- audit rows contain request metadata but no complete API Key.

`assert_application_references.sql` uses `LEFT JOIN ... WHERE parent.id IS NULL` checks for every logical reference column removed by V2 and raises a nonzero assertion result when an orphan exists. It performs no update or delete.

- [ ] **Step 2: Run unit tests and integration tests to expose remaining failures**

Run: `.\mvnw.cmd test`

Expected: PASS for unit tests.

Run: `.\mvnw.cmd -Pintegration verify`

Expected: FAIL until audit wiring and any cross-module integration gaps are completed.

- [ ] **Step 3: Implement independent audit persistence and OpenAPI metadata**

`ApiAuditFilter` wraps request and response with Spring content-caching wrappers, records start time, delegates once, copies the response body back, and calls `ApiAuditService.record` in `REQUIRES_NEW`. Parse `errorCode` only from the common JSON envelope. Hash the normalized request body; never store headers containing the API Key. Catch and log audit persistence failure without changing the HTTP response.

Define an OpenAPI `SecurityScheme` named `ApiKeyAuth`:

```java
new SecurityScheme()
    .type(SecurityScheme.Type.APIKEY)
    .in(SecurityScheme.In.HEADER)
    .name("X-API-Key");
```

Document `X-Request-Id`, `Idempotency-Key`, the common error envelope, all eight paths, and enum values. Do not place a real key in OpenAPI examples.

- [ ] **Step 4: Add runbook documentation and execute the full verification matrix**

`.env.example` contains only names and clearly fake values. `README.md` must document:

1. Java 21, Maven/Docker/MySQL prerequisites;
2. environment variables and local profile;
3. `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`;
4. Flyway V1-V3 behavior;
5. one curl example for each of the eight endpoints;
6. unit and integration test commands;
7. Swagger UI and health URLs;
8. the fact that local standards are examples, not official relief policy;
9. MVP exclusions and no-GIS population-overlap warning.

Run:

```bash
.\mvnw.cmd clean test
.\mvnw.cmd -Pintegration verify
powershell -ExecutionPolicy Bypass -File scripts/mysql/tests/test-wrapper-preconditions.ps1
mysql --host=127.0.0.1 --port=3306 --user=root --password flood_scenario_deduction --execute="source sql/mysql/tests/assert_application_references.sql"
git diff --check
git status --short
```

Expected: all executable tests pass; `git diff --check` emits no whitespace error; `git status` still shows the unrelated pre-existing `D test` unless the user has handled it separately.

- [ ] **Step 5: Run a local MySQL smoke test**

With local environment variables set, start the application against `127.0.0.1:3306/flood_scenario_deduction`, call health, create one event, assess it, and calculate material demand. Confirm `api_audit_log`, assessment, and calculation rows exist and no full API Key appears in logs.

Expected: health is UP; each first write returns success; reusing the same operation/key/body returns the stored resource; all business records are internally consistent.

- [ ] **Step 6: Commit the completed API service**

```bash
git add .gitignore .env.example README.md src pom.xml sql/mysql sql/mysql-local
git commit -m "feat: complete flood situation and material open api"
```

Do not stage the unrelated `test` deletion.

---

## Plan Self-Review Record

- **Specification coverage:** Tasks 1-12 cover project bootstrap, V1-V3 migrations, all eight endpoints, API Key/scope/rate limiting, request tracing, idempotency, event validation, situation rules, material formulas, no-FK integrity, snapshots, auditing, OpenAPI, local/test seeds, unit tests, MySQL integration tests, and operator documentation.
- **Out-of-scope guard:** No task introduces GIS, hydraulic simulation, JWT/OAuth2, management APIs, messaging, asynchronous processing, or distributed rate limiting.
- **Type consistency:** Region identifiers are external `region_code` strings at API boundaries and numeric IDs internally. Event/assessment/calculation public IDs are strings; database primary keys remain `long`. All time enters as `OffsetDateTime`, becomes `Instant`, and persists as UTC. All quantities use `BigDecimal`.
- **Transaction consistency:** Six write/calculation controllers enter through one `IdempotencyExecutor` transaction. Event import plus assessment evidence is atomic. Material headers and details are atomic. Audit writes use a separate transaction and cannot change business outcomes.
- **Placeholder scan:** The plan contains no deferred implementation markers; each task names concrete files, interfaces, tests, commands, outcomes, and commit boundaries.
