# Week 14 — Testing & Integration

**Week 14** · Testing critical Roost paths

## 1. Definition & Why

Week 14 focused on testing the Roost application at multiple levels instead of relying only on manual API testing.

The goal was to learn how to test:
- business logic without Spring or a database,
- service logic with mocked dependencies,
- REST controllers as an MVC slice,
- the complete application with a real PostgreSQL database,
- and concurrent requests against the real application and database.

The important idea is that these are **different testing levels**, not competing alternatives. Each level answers a different question.

## 2. Mental Model

Think of the testing pyramid as layers:

```text
                    Full Integration
                  @SpringBootTest
                       ▲
                       │
              Testcontainers + DB
                       ▲
                       │
                Controller Slice
                  @WebMvcTest
                       ▲
                       │
              Service Unit Tests
                  Mockito
                       ▲
                       │
              Pure Unit Tests
                    JUnit 5
```

Lower layers are generally faster and more isolated. Higher layers verify more real application wiring and infrastructure, but are slower.

For Roost:

```text
JUnit 5
  → Does calculateUptimePercent() behave correctly?

Mockito
  → Does MonitorService behave correctly with controlled dependencies?

@WebMvcTest
  → Does MonitorController correctly handle HTTP, validation,
    responses, and errors?

@SpringBootTest + Testcontainers
  → Does the real application work through HTTP with PostgreSQL?

Concurrency integration test
  → Can multiple HTTP requests write probe results concurrently
    without lost probe writes?
```

## 3. What We Built

### 3.1 JUnit 5 unit tests

Added and tested `MonitorService.calculateUptimePercent(...)`.

Cases covered:
- all probes UP
- all probes DOWN
- mixed UP/DOWN
- empty list
- unsupported status
- null-related behavior
- parameterized inputs

Representative code:

```java
long upCount = probeResults.stream()
        .filter(probeResult -> "UP".equals(probeResult.getStatus()))
        .count();

return (upCount * 100.0) / probeResults.size();
```

**Purpose:** test business logic in isolation.

### 3.2 Mockito service tests

`MonitorServiceTest` uses mocked repositories and `AuditService` with `@InjectMocks`.

Tests covered:
- existing monitor lookup
- monitor creation
- monitor-not-found behavior

Important concepts:
- `@Mock`
- `@InjectMocks`
- `when(...).thenReturn(...)`
- `verify(...)`
- argument matchers such as `any(...)` and `eq(...)`
- strict stubbing
- `thenAnswer(...)` for controlled mock return behavior

**Purpose:** test service/business behavior while replacing database repositories and collaborators with controlled mocks.

### 3.3 Controller slice test

Created `MonitorControllerTest` using:

```java
@WebMvcTest(MonitorController.class)
@AutoConfigureMockMvc(addFilters = false)
```

with a mocked `MonitorService`.

Tests covered:
- successful GET → 200
- monitor not found → 404
- invalid POST request → 400

**Purpose:** test the web/controller layer without starting the complete application or connecting to a real database.

### 3.4 Full integration test with Testcontainers

Created `MonitorPersistenceIntegrationTest` using:

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
```

A real PostgreSQL 17 container is started for the test. Spring receives the container's connection details through `@DynamicPropertySource`.

The application starts normally with:
- Spring Boot context
- embedded Tomcat
- JPA/Hibernate
- repositories
- services
- controllers
- Flyway
- PostgreSQL

### 3.5 Real persistence round-trip

The integration test verifies:

```text
POST /workspaces
       ↓
POST /monitors
       ↓
POST /monitors/{id}/probe-results
       ↓
PostgreSQL
       ↓
GET /monitors/{id}
       ↓
Persisted probe result returned
```

This verifies real HTTP → Spring → JPA → PostgreSQL behavior rather than mocked interactions.

### 3.6 Concurrent integration test

Added `concurrentProbeWrites_persistAllResults()`.

The test creates one monitor and launches 20 worker threads. Each worker sends:

```text
POST /monitors/{monitorId}/probe-results
```

`CountDownLatch` coordinates the workers so they are released together.

After all requests finish, the test verifies:

```java
assertEquals(20,
        probeResultRepository.countByMonitorId(monitorId));
```

and:

```java
assertEquals(
        20,
        auditLogRepository.countByMonitorIdAndAction(
                monitorId,
                "PROBE_RESULT_RECORDED"
        )
);
```

**What it proves:** 20 concurrent HTTP requests completed successfully and all 20 probe records plus their corresponding audit records were persisted.

**What it does not prove:** strict idempotency or duplicate prevention, because the API has no request ID/unique-key mechanism defining when two requests are duplicates. It also tests concurrent inserts rather than concurrent updates.

## 4. The Most Important Concept: These Tests Are Different Levels

They are not five different ways of testing exactly the same thing.

| Test | Main target | Spring context? | Real DB? | Typical speed |
|---|---|---:|---:|---|
| JUnit 5 unit | Pure business logic | No | No | Very fast |
| Mockito unit | Service logic | No | No | Fast |
| `@WebMvcTest` | Controller/web layer | Partial | No | Fast |
| `@SpringBootTest` | Application integration | Yes | Can be real | Slower |
| Testcontainers concurrency | Real application + DB under concurrency | Yes | Yes | Slowest |

The boundaries can overlap, but the question being tested is different.

## 5. What Is Actually Used in Real Projects?

All of these are used in real projects. A production codebase normally uses a mix.

### Foundation: JUnit 5 unit tests

Usually the largest group because they are fast, deterministic, cheap to run, and easy to debug.

### Service tests with Mockito

Very common in Spring Boot applications for testing service/business behavior while isolating repositories and other collaborators.

### Controller tests with `@WebMvcTest`

Useful for request mapping, validation, JSON mapping, status codes, and controller exception behavior.

### Integration tests with `@SpringBootTest`

Used for critical paths where Spring wiring and infrastructure need to be verified together.

### Testcontainers

Useful when behavior depends on real infrastructure such as PostgreSQL, MySQL, Redis, Kafka, or Elasticsearch. It is especially valuable when an embedded database or mock cannot accurately reproduce production behavior.

### Concurrency tests

Specialized rather than universal. They make sense for concurrency-sensitive operations such as concurrent writes, race conditions, locking, unique constraints, counters, job processing, and event consumption.

## 6. What You Should Concentrate on Most

### 1. JUnit 5 — foundation

Be comfortable with:
- assertions
- Arrange → Act → Assert
- test structure
- parameterized tests
- edge cases
- testing pure business logic

### 2. Mockito — very important

Know:
- `@Mock`
- `@InjectMocks`
- `when(...).thenReturn(...)`
- `verify(...)`
- `any(...)`
- `eq(...)`
- `thenAnswer(...)`
- strict stubbing
- mock vs spy
- when mocking is useful and when it becomes excessive

Mental model:

```text
Service
 ├── Repository → mock
 ├── Repository → mock
 └── AuditService → mock

Test only the Service behavior.
```

### 3. Integration testing with real infrastructure — very important

For Roost, this is particularly valuable.

Understand:

```text
@SpringBootTest
      +
Testcontainers
      +
real PostgreSQL
```

Be able to explain:
- application context
- random server port
- TestRestTemplate
- Testcontainers
- DynamicPropertySource
- Flyway migrations
- real database persistence
- transaction behavior

### 4. `@WebMvcTest` — important but narrower

Remember:

```text
@WebMvcTest
    = controller/web-layer slice test
```

versus:

```text
@SpringBootTest
    = full application integration test
```

### 5. Concurrency testing — understand the concept

Know the purpose of:
- thread pools
- concurrent requests
- `CountDownLatch`
- race conditions
- lost writes
- transaction boundaries

You do not need to memorize all `ExecutorService` boilerplate.

## 7. Important Lessons From Our Debugging

### Security can affect test slices

The MVC test initially returned 401 because security filters were active. We used:

```java
@AutoConfigureMockMvc(addFilters = false)
```

This was test-specific and did not disable application security globally.

### Mockito matchers must be used consistently

Mixing matcher and raw arguments incorrectly can cause `InvalidUseOfMatchersException`.

### Unstubbed mocks return defaults

The MVC test initially failed because `getProbeResults(...)` was not stubbed and the controller received `null` where it expected a `Page`.

### Integration tests can expose API serialization issues

Trying to deserialize the monitor detail response directly into a DTO containing `Page<ProbeResultResponse>` exposed a Jackson/deserialization problem. The integration test was changed to inspect the real HTTP body as a `String` rather than changing production API design solely for the test.

### Testcontainers provides isolated infrastructure

The integration test uses its own temporary PostgreSQL container rather than the existing local Docker Compose PostgreSQL instance.

## 8. Common Mistakes

- Testing implementation details instead of behavior.
- Mocking everything, including the thing you actually need to verify.
- Using `@SpringBootTest` for every small unit test.
- Using only mocks and never testing real database integration.
- Assuming a controller slice test proves database persistence.
- Assuming an integration test proves every business-rule edge case.
- Writing concurrency tests everywhere without an actual concurrency risk.
- Treating 20 successful concurrent inserts as an idempotency test.
- Adding production configuration solely to make a test pass without understanding why.
- Confusing warnings with failures.

## 9. Quick Revision + Q&A

**Revision (30 sec):**

```text
JUnit
→ test logic

Mockito
→ test service behavior in isolation

@WebMvcTest
→ test controller/web layer

@SpringBootTest
→ test the application together

Testcontainers
→ test against real infrastructure

Concurrency integration test
→ test behavior under concurrent requests
```

The important skill is choosing the smallest test level that can prove the behavior you care about.

**Q&A:**

- **Why not use `@SpringBootTest` for everything?** It is slower and less isolated than unit or slice tests.
- **Why not use only unit tests?** Unit tests cannot prove that Spring wiring, JPA mappings, Flyway migrations, SQL, or PostgreSQL behavior work together.
- **Why use Testcontainers?** To exercise real external infrastructure in a repeatable test environment.
- **What does `@WebMvcTest` isolate?** The MVC/web layer.
- **What does Mockito isolate?** The class under test from its collaborators.
- **What does the concurrency test add?** It checks a behavior that ordinary sequential tests cannot reproduce: concurrent probe writes.

## 10. Key Takeaway

Week 14's central lesson is **test boundaries**.

```text
Need fast business-rule coverage?
→ Unit test

Need service behavior without DB?
→ Mockito

Need HTTP/controller coverage?
→ @WebMvcTest

Need real Spring + DB integration?
→ @SpringBootTest + Testcontainers

Need concurrency coverage?
→ Targeted concurrency integration test
```

A strong Spring Boot test suite uses these levels together rather than choosing only one.
