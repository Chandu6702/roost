# Roost — Day-by-Day Self-Contained Plan (Weeks 10–19)

> **Use alone:** each day = one task. Study the resource (~20 min cap), **build the day's Roost feature** (~25 min), self-check against the **Answers** below the questions, write the 6-section note, tick the checkbox, commit, push. Wrong/shaky answer → `mistakes.md`.

> **Primary resources:** Amigoscode Spring Boot (YouTube) · Chad Darby (Udemy) · Baeldung Spring articles · Spring docs · _Spring Start Here_ (Spilcă). If a link 404s, search the title.

> **Method:** Every learning example ships as a real Roost feature. No `Book`/`Author` demos. The Spring Boot project already exists at `roost/` — code lands there from Day 1.

> **⚠ Spring Boot 4 notice:** we're on SB4.1. If a tutorial says `spring-boot-starter-web`, our pom already uses `spring-boot-starter-webmvc` — same starter, new name. Everything else is 95% identical to SB3.

## Weekly rhythm (reminder)

- **Tue–Sat evening:** ~60 min. Read entry → 20 min resource → 25 min code → 10 min self-check → 5 min note+commit.
- **Monday morning (~2.5 hrs):** week's hardest Roost feature.
- **Monday evening (~1.5–2 hrs):** recall test → confidence scores → `interview-qa/week-NN-*.md` → plan next week → DSA re-solve.
- **Sunday:** full rest, never scheduled.
- **Minimum viable day:** 10 min recall + 1 DSA problem. Never let a missed day become two.

## Solo method & re-entry (if without mentor access)

Work the day yourself: read resource → code the deliverable → answer the questions from memory → check against the Answers here → note anything wrong in `mistakes.md` with a `(solo — verify with mentor)` tag. On the next mentored session, run the re-entry protocol: quiz everything solo, resolve `(solo — verify)` tags.

<!-- --- -->

# MONTH 3 — Spring core, REST, JPA, transactions

## WEEK 10 — Spring core, wired into Roost from Day 1

**Goal by end of week:** Roost skeleton runs, one real endpoint (`POST /workspaces`) is wired through service + config + validation; app starts on both `dev` and `prod` profiles with different config.

### ✅ Day 1 — IoC & Dependency Injection (plain Java, inside the Spring project)

**Resource:** Amigoscode "Intro to Spring Boot & DI" (first ~20 min) OR Baeldung "Intro to Inversion of Control and Dependency Injection with Spring".
**Code:** in `src/main/java/com/chandu/roost/probe/`, create two plain Java classes (no Spring annotations yet): `HttpProbeClient` (with one method `probe(String url)` returning a stub result) and `ProbeRunner` that receives `HttpProbeClient` via **constructor**. Add a plain unit test in `src/test/java/.../probe/ProbeRunnerTest.java` that swaps in a `MockHttpProbeClient` — proves loose coupling works. **No Spring annotations today.** On Day 3 you'll add `@Service` and see the container take over.
**Self-check:** 1) What is IoC in one sentence? 2) DI is a _specific form_ of IoC — of what? 3) Constructor vs field injection — which is better and why? 4) Why does DI make code testable? 5) What is the "service locator" anti-pattern and why is DI preferred?
**Answers:** 1) The framework calls your code, not the other way — you don't `new` collaborators, they're given to you. 2) Of _dependency management_ — the container decides who gets what. 3) Constructor: dependencies are final, class is immutable, easy to test (pass mocks directly), and compiler enforces required deps. Field injection hides deps and needs reflection to test. 4) Because you can pass a fake/mock in the constructor without touching the class under test. 5) Service locator: class asks a global registry for its deps (`Locator.get(HttpProbeClient.class)`). Hides deps, hard to test, ties every class to the locator. DI inverts this — deps are declared, container provides.

### ✅ Day 2 — First Roost endpoint (health) + application.properties

**Resource:** Spring guide "Building a RESTful Web Service" (skim, 15 min).
**Code:** create `com.chandu.roost.health.HealthController` with `@RestController`, one `GET /health` returning `Map.of("status", "up", "app", "roost")`. Run `./mvnw spring-boot:run`, curl `http://localhost:8080/health`. Rename `application.properties` → `application.yml`, add `spring.application.name: roost` and `server.port: 8080`.
**Self-check:** 1) What does `@SpringBootApplication` combine? 2) What server runs by default in SB4 and where's it defined? 3) Difference between `@Controller` and `@RestController`? 4) What is auto-configuration? 5) Why does the app start even though we didn't wire anything ourselves?
**Answers:** 1) `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` on the current package downwards. 2) Embedded Tomcat; brought in transitively by `spring-boot-starter-webmvc`. 3) `@RestController` = `@Controller` + `@ResponseBody` — every handler serializes its return value to JSON directly. `@Controller` alone expects a view name. 4) SB scans the classpath, sees `spring-webmvc` present, and configures Tomcat + `DispatcherServlet` + JSON message converters without you writing a bean. 5) Because starters bring transitive deps, and auto-config classes (`WebMvcAutoConfiguration`, `EmbeddedTomcatAutoConfiguration`, etc.) fire on classpath conditions.

### ✅ Day 3 — Beans, `@Service`, component scanning

**Resource:** Baeldung "Spring Bean Annotations" + "Component Scan".
**Code:** promote yesterday's plain `HttpProbeClient` and `ProbeRunner` to Spring beans — add `@Component` (or `@Service`) to both. Create `com.chandu.roost.workspace.WorkspaceService` (`@Service`) with `createWorkspace(String name)` returning a stub `Workspace` record. Create `WorkspaceController` (`@RestController`, `POST /workspaces` accepting a `CreateWorkspaceRequest` record, returning 201). Wire via constructor. Confirm `curl -X POST -H 'Content-Type: application/json' -d '{"name":"Acme"}' http://localhost:8080/workspaces` returns the stub.
**Self-check:** 1) `@Component` vs `@Service` vs `@Repository` — what's the actual difference? 2) When are beans instantiated by default? 3) What if two beans implement the same interface — how does Spring pick? 4) `@Qualifier` vs `@Primary` — when do you use each? 5) What does `@ComponentScan` scan by default in a `@SpringBootApplication` class?
**Answers:** 1) Functionally identical for DI — all are stereotype annotations. `@Service` and `@Repository` add semantic meaning (business logic / data access); `@Repository` also enables Spring's translation of persistence exceptions to `DataAccessException`. 2) At app startup (eagerly for singleton scope) unless marked `@Lazy`. 3) It fails to start with `NoUniqueBeanDefinitionException` — unless one is `@Primary` or you disambiguate at the injection point with `@Qualifier("name")`. 4) `@Primary` on one bean = default choice for injection when multiple candidates exist (whole-app decision). `@Qualifier` picks at the specific injection point (per-injection). 5) The package of the class annotated with `@SpringBootApplication` and everything below it.

### ✅ Day 4 — Config: properties, profiles, `@ConfigurationProperties`

**Resource:** Baeldung "Guide to `@ConfigurationProperties`" + "Spring Profiles".
**Code:** in `application.yml` add:

```yaml
roost:
  probe:
    default-interval-seconds: 60
    default-timeout-millis: 5000
```

Create `com.chandu.roost.config.ProbeProperties` as a Java record annotated `@ConfigurationProperties(prefix = "roost.probe")`. Register it via `@EnableConfigurationProperties(ProbeProperties.class)` on your `RoostApplication`. Inject into `ProbeRunner` and log the values at `@PostConstruct`. Add `application-dev.yml` and `application-prod.yml` with different `default-interval-seconds` values (dev = 30, prod = 300). Run with `--spring.profiles.active=dev` then `prod`, confirm the log differs.
**Self-check:** 1) `@Value("${x}")` vs `@ConfigurationProperties` — when do you use each? 2) What is "relaxed binding"? 3) How do profile-specific yml files override the base? 4) How is a Kubernetes-style env var (`ROOST_PROBE_DEFAULT_INTERVAL_SECONDS=10`) mapped? 5) Where do profile-specific properties come from in prod (yml file vs env var vs config server)?
**Answers:** 1) `@Value` for one or two individual props; `@ConfigurationProperties` for a group with validation, IDE autocomplete, and record immutability. 2) Property key `roost.probe.default-interval-seconds` matches Java field `defaultIntervalSeconds`, kebab/snake/camel/underscore all bind. 3) `application-<profile>.yml` is loaded on top of `application.yml`; keys in the profile file override. If `roost.probe.default-interval-seconds` exists in both, the profile wins. 4) Env vars use `SCREAMING_SNAKE_CASE` and map by relaxed binding to `roost.probe.default-interval-seconds`. Underscores between segments become dots. 5) In prod: env vars (via k8s secrets/configmaps) or an external config source (Spring Cloud Config, Vault). Never commit prod secrets; `application-prod.yml` should hold defaults, not secrets.

### ☐ Day 5 — Week 10 consolidation + first Flyway migration

**Resource:** Baeldung "Flyway Migrations with Spring Boot" (skim, 10 min).
**Code:** stand up Postgres via docker-compose (write `docker-compose.yml` at repo root — `postgres:17`, exposed on 5432, POSTGRES_USER/PASSWORD/DB set to `roost`). Update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/roost
    username: roost
    password: roost
  flyway:
    enabled: true
```

Create `src/main/resources/db/migration/V1__workspaces.sql`:

```sql
CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Boot the app, confirm Flyway ran (table exists, `flyway_schema_history` populated). **No JPA entities yet — that lands W12.** Write `notes/10-spring-core/spring-core.md` (Feynman close: explain IoC in 60 sec). Commit + push.
**Self-check:** Mixed Qs across IoC, beans, config, profiles. + What does Flyway do that `ddl-auto=update` doesn't? Why should we prefer Flyway from Day 1?
**Answers:** Flyway = versioned, ordered, immutable schema history. Every environment applies the same migrations in the same order; you can prove what schema prod is on; migrations code-review just like app code. `ddl-auto=update` guesses schema changes at startup from JPA entities — non-reproducible, can't handle destructive changes (drops, renames), can't add indexes or seed data, no history table.

### ☐ Monday W10 — deep-work morning + weekly review evening

**Morning (~2.5 hrs):** clean up W10 code — refactor packages (`workspace/`, `probe/`, `health/`, `config/`), add proper `application-dev.yml` / `application-prod.yml`, verify `docker-compose up -d db && ./mvnw spring-boot:run` works from a fresh checkout.
**Evening (~1.5 hrs):** recall test on IoC/DI/beans/config/Flyway → confidence scores in `reviews/week-10-review.md` → generate `interview-qa/week-10-spring-core.md` → plan W11 → DSA re-solve.

---

## WEEK 11 — REST layer on Roost

**Goal by end of week:** `MonitorController` CRUD end-to-end with DTOs, validation, global error handling, pagination, and Swagger UI live at `/swagger-ui.html`.

### ☐ Day 1 — `@RestController`, request mapping, HTTP verbs

**Resource:** Baeldung "Spring `@RequestMapping`" + "Spring REST API Tutorial".
**Code:** create `com.chandu.roost.monitor.MonitorController` with 5 endpoints: `GET /monitors`, `GET /monitors/{id}`, `POST /monitors`, `PUT /monitors/{id}`, `DELETE /monitors/{id}`. Back with an in-memory `Map<Long, Monitor>` in a `MonitorService` (Postgres wiring comes W12). Test with curl or IntelliJ HTTP client.
**Self-check:** 1) Difference between `@RequestMapping(method=GET)` and `@GetMapping`? 2) `@PathVariable` vs `@RequestParam` vs `@RequestBody`? 3) When would you return `ResponseEntity` vs a plain object? 4) Correct HTTP status code for successful `POST /monitors`? For `DELETE`? For "monitor not found on GET"? 5) What does `@RequestMapping` do at class level vs method level?
**Answers:** 1) Shortcut — `@GetMapping` = `@RequestMapping(method = RequestMethod.GET)`. Prefer the shortcuts. 2) Path variable = part of URL (`/monitors/{id}`). Request param = query string (`?status=UP`). Request body = JSON payload deserialized. 3) `ResponseEntity` when you need to control status code and/or headers (e.g., 201 + `Location` header on create). Plain object = 200 OK. 4) 201 Created (with `Location: /monitors/{id}` header ideally); 204 No Content for DELETE; 404 Not Found. 5) Class-level `@RequestMapping("/monitors")` = base path; method-level adds sub-path.

### ☐ Day 2 — DTOs, Jackson, records

**Resource:** Baeldung "Java Records" + "Jackson with Spring".
**Code:** split `Monitor` (domain entity — has all internal state) from `MonitorRequest` (client input) and `MonitorResponse` (client output) — both as Java records. `MonitorService.create(MonitorRequest)` maps input → entity; `MonitorController` maps entity → response. Use `@JsonProperty("check_interval_seconds")` on one field to see snake*case in JSON while keeping camelCase in Java.
**Self-check:** 1) Why separate DTOs from entities — what breaks if you use the entity everywhere? 2) Why records for DTOs — what do they give you? 3) `@JsonIgnore` vs `@JsonProperty(access = READ_ONLY)` — difference? 4) What happens if the client sends an unknown JSON field? How do you fail loud vs silent? 5) When Jackson deserializes into a record, how does it find the constructor?
**Answers:** 1) Entity leaks internal fields (audit timestamps, tenant ID, versioning); ties JSON schema to DB schema — any DB refactor breaks the API contract; adds circular JSON on relationships. DTOs isolate the contract. 2) Immutability, `equals`/`hashCode`/`toString` free, compact syntax, safer as request/response objects. 3) `@JsonIgnore` = field never serialized nor deserialized. `@JsonProperty(access = READ_ONLY)` = serialized (in responses) but \_ignored* on deserialization (so a client can't set `id`). 4) Default: ignored silently. Fail loud: `spring.jackson.deserialization.fail-on-unknown-properties: true` in yml, or `@JsonIgnoreProperties(ignoreUnknown = false)` per class. 5) Jackson uses the canonical constructor of the record; annotate parameters or use `@JsonCreator` if names don't match JSON keys.

### ☐ Day 3 — Bean Validation on `MonitorRequest`

**Resource:** Baeldung "Validation in Spring Boot" (Jakarta Bean Validation).
**Code:** on `MonitorRequest` add: `@NotBlank` name, `@NotBlank @URL` url, `@Min(30) @Max(3600)` intervalSeconds, `@NotNull` type (enum). Add `@Valid` at the `@RequestBody` in the controller. Hit `POST /monitors` with an invalid body, inspect the raw 400 (ugly today — fixed tomorrow).
**Self-check:** 1) Difference between `@NotNull`, `@NotEmpty`, `@NotBlank`? 2) Where do validation constraints belong — DTO or entity? 3) What exception is thrown on validation failure and when? 4) How would you validate nested objects (e.g., `MonitorRequest` contains a `NotificationChannelRequest`)? 5) How do you write a custom constraint (e.g., `@ValidSlug`)?
**Answers:** 1) `@NotNull` = must not be null (empty string OK). `@NotEmpty` = null and empty rejected (`""`, `[]`, `{}` rejected). `@NotBlank` = strings only, must have non-whitespace content. 2) DTO — validation belongs at the boundary (input validation). Entities have their own domain invariants which are stricter. 3) `MethodArgumentNotValidException`, thrown before your handler executes — Spring runs validators as part of argument resolution. 4) `@Valid` on the nested field: `@Valid NotificationChannelRequest notification`. 5) Create annotation `@ValidSlug` + `ConstraintValidator<ValidSlug, String>` implementing `isValid`. Register with `@Constraint(validatedBy = SlugValidator.class)`.

### ☐ Day 4 — Global error handling with `@RestControllerAdvice`

**Resource:** Baeldung "Exception Handling in Spring" (RestControllerAdvice section) + Spring docs "ProblemDetail (RFC 7807)".
**Code:** `com.chandu.roost.error.GlobalExceptionHandler` annotated `@RestControllerAdvice`, handling: `MethodArgumentNotValidException` → 400 with a `ProblemDetail` body listing field errors; `MonitorNotFoundException` (custom `RuntimeException`, throw from service when id absent) → 404; catch-all `Exception` → 500 with a generic message (don't leak stack traces). Body schema: `{ type, title, status, detail, timestamp, path, fieldErrors? }`.
**Self-check:** 1) Order of resolution when both a controller-local `@ExceptionHandler` and `@RestControllerAdvice` handle the same exception? 2) Why is a global handler better than try/catch in every controller? 3) What's `ProblemDetail` (RFC 7807)? 4) Should you log the stack trace of a 4xx? Of a 5xx? 5) How would you return the same error format for a validation failure vs a business rule failure?
**Answers:** 1) Controller-local wins over advice; advice is fallback. 2) DRY, consistent client contract, no exception can accidentally escape the handler chain and become a raw 500 with a leaked stack. 3) Standard JSON schema for errors (`{ type, title, status, detail, instance }`). Built into Spring 6+. Prefer over inventing your own. 4) 4xx = client's fault, log at INFO/DEBUG (don't fill logs with predictable client mistakes). 5xx = your fault, log at ERROR with full stack trace. 5) Both funnel through `@RestControllerAdvice` — validation handler and business-exception handler both build the same `ProblemDetail` shape.

### ☐ Day 5 — Week 11 consolidation + Swagger/OpenAPI

**Resource:** springdoc-openapi docs "Getting Started".
**Code:** add dep `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0` (verify latest for SB4 compatibility — search "springdoc SB4" if issues). Restart app, hit `http://localhost:8080/swagger-ui.html` — you should see all `/monitors` endpoints. Annotate `MonitorController` with `@Tag(name = "Monitors")`, one endpoint with `@Operation(summary = "...")`, one field on `MonitorRequest` with `@Schema(description = "...")`. Add manual pagination to `GET /monitors`: `?page=0&size=10&sort=name,asc`, return `{ content: [...], page, size, totalElements }` (compute manually — real `Page<>` comes in W12 with JPA).
**Self-check:** Mixed Qs from the week + one on why Swagger docs matter for a "real" project (recruiter/reviewer POV).
**Answers:** Swagger UI = you can prove the API works without running curl; recruiter clicks Swagger link → instant credibility. Also self-documenting: the JSON schema for every DTO is discoverable without reading Java code. In interviews, "how do you document APIs?" — you say "OpenAPI 3, springdoc, JWT wired into the auth scheme" (that last part lands in W15).

### ☐ Monday W11 — deep-work + review

**Morning (~2.5 hrs):** implement a second controller (`NotificationChannelController` — same pattern), fully tested with curl. Consolidates the CRUD muscle.
**Evening (~1.5 hrs):** recall test → `reviews/week-11-review.md` → `interview-qa/week-11-rest-layer.md` → plan W12.

---

## WEEK 12 — Data layer (JPA + Postgres + Flyway from Day 1)

**Goal by end of week:** monitors are persisted in Postgres, retrievable via Spring Data repositories, related to workspaces and probe results; you have hit N+1 and fixed it; you have EXPLAIN ANALYZE'd a real query and added an index.

### ☐ Day 1 — JPA + first `@Entity` + Flyway migration

**Resource:** Baeldung "A Guide to JPA with Spring".
**Code:** write `V2__monitors.sql` (create table `monitors` with FK to `workspaces`, columns `id`, `workspace_id`, `name`, `url`, `type`, `interval_seconds`, `timeout_millis`, `created_at`). Create `com.chandu.roost.workspace.Workspace` and `com.chandu.roost.monitor.Monitor` `@Entity` classes matching. Use `@Table`, `@Id @GeneratedValue`, `@Column(nullable = false)`. Set `spring.jpa.hibernate.ddl-auto: validate` (not `update` — Flyway owns schema). Boot the app, confirm no schema errors.
**Self-check:** 1) Why `ddl-auto: validate` and not `update` when using Flyway? 2) `@GeneratedValue(strategy = IDENTITY)` vs `SEQUENCE` on Postgres? 3) What does `@Column(nullable = false)` do at runtime vs at schema-generation time? 4) Where should JPA annotations live — on fields or getters? 5) Why don't records make good JPA entities?
**Answers:** 1) `validate` = Hibernate checks entities match the schema Flyway created and fails startup on mismatch. `update` would try to alter the schema itself, competing with Flyway → chaos. 2) `IDENTITY` = uses Postgres `BIGSERIAL` (auto-increment column). `SEQUENCE` = uses an explicit Postgres sequence, allows batching (Hibernate can pre-fetch IDs). `SEQUENCE` is usually preferred for bulk inserts. 3) Runtime: Hibernate throws before flushing. Schema-gen time (if you were using `create`): adds NOT NULL to the DDL. With Flyway you control DDL — the annotation only matters for validation and for Hibernate's own null checks. 4) Fields — modern convention, cleaner. Was historically getters (Hibernate uses reflection on either). 5) Records are immutable + final — JPA needs a no-arg constructor + settable fields for lazy loading proxies. Use classes.

### ☐ Day 2 — Spring Data JPA repositories

**Resource:** Baeldung "Introduction to Spring Data JPA".
**Code:** `WorkspaceRepository extends JpaRepository<Workspace, Long>` with `Optional<Workspace> findBySlug(String slug)`. `MonitorRepository extends JpaRepository<Monitor, Long>` with `List<Monitor> findByWorkspaceId(Long workspaceId)`, `List<Monitor> findByWorkspaceIdAndNameContainingIgnoreCase(Long workspaceId, String q)`. Wire into services; replace the in-memory `Map` in `MonitorService`.
**Self-check:** 1) How does Spring turn `findByWorkspaceIdAndNameContainingIgnoreCase` into SQL — literally, what happens? 2) What if you name a method wrong? 3) When would you write `@Query("...JPQL...")` vs `@Query(nativeQuery = true)`? 4) What does `JpaRepository` give you that `CrudRepository` doesn't? 5) How do you paginate: `findByWorkspaceId(Long id, Pageable pageable)` — what does it return?
**Answers:** 1) Method-name parser splits by `And`/`Or`/`OrderBy`; each token maps to an entity field; keywords like `Containing`, `IgnoreCase`, `GreaterThan` map to SQL operators. Startup-time — invalid names fail the app boot with a clear error. 2) App fails at startup, not at request time. 3) JPQL when it's still your entity model but too complex for method names (joins, subqueries). Native when you need Postgres-specific features (`ON CONFLICT`, window functions, JSONB ops). 4) `JpaRepository` adds pagination + sorting (`Pageable`), batch save, flush, and entity-manager access. `CrudRepository` is the minimum. 5) Returns `Page<Monitor>` — contains `content` list plus metadata (total elements, total pages, current page).

### ☐ Day 3 — Relationships: `@ManyToOne`, `@OneToMany`, `@ManyToMany`

**Resource:** Baeldung "JPA Entity Relationships" + "Best Practice for Many-to-One and One-to-Many".
**Code:** on `Monitor` add `@ManyToOne @JoinColumn(name = "workspace_id") Workspace workspace` (owning side). On `Workspace` add `@OneToMany(mappedBy = "workspace") List<Monitor> monitors` (inverse). Write `V3__probe_results.sql` (table `probe_results` with FK to `monitors`, columns `probed_at`, `status`, `latency_ms`, `error_message`). Create `ProbeResult` entity with `@ManyToOne Monitor monitor`. Add helper `Monitor.addProbeResult(ProbeResult)` that sets both sides. Persist a workspace + 2 monitors + 5 probe results in a test.
**Self-check:** 1) Which side is the "owning" side in `@ManyToOne`/`@OneToMany`? Why? 2) What breaks if you set only one side of a bidirectional relationship in memory? 3) `fetch = LAZY` vs `EAGER` — the default for `@ManyToOne` vs `@OneToMany`? 4) What is `LazyInitializationException` and when does it happen? 5) What is `orphanRemoval = true`?
**Answers:** 1) The `@ManyToOne` side owns — it holds the FK column. `@OneToMany(mappedBy = ...)` is inverse — no FK, no ownership. 2) In-memory state disagrees with DB semantics; after `save+flush`, one side reflects DB (correct), the other still holds stale reference. That's why you write helper methods (`addProbeResult`) — they set both sides. 3) `@ManyToOne` default = EAGER (bad — creates surprise joins). `@OneToMany` default = LAZY (good). Best practice: set `@ManyToOne(fetch = LAZY)` explicitly always. 4) Accessing a lazy field after the persistence context (transaction) has closed. Common when returning a JPA entity from a `@Transactional` service and trying to touch a lazy field in the controller. Fix: fetch what you need inside the transaction, or use a DTO projection, or `@EntityGraph`. 5) When you remove a child from the parent's collection, JPA deletes the child row. Without it, you'd have to delete manually.

### ☐ Day 4 — The N+1 problem and fixing it

**Resource:** Vlad Mihalcea "N+1 Query Problem" (blog) + Baeldung "Spring Data JPA `@EntityGraph`".
**Code:** enable `spring.jpa.show-sql: true` and `logging.level.org.hibernate.SQL: DEBUG`. Write a service method that loads all workspaces then iterates each workspace's `monitors` collection — watch 1 + N queries fire in the log. Fix two ways: (a) `@Query("select w from Workspace w join fetch w.monitors")`, (b) `@EntityGraph(attributePaths = "monitors")` on a repo method. Add both, compare SQL logs.
**Self-check:** 1) What's the N+1 problem in one sentence? 2) `JOIN FETCH` vs `@EntityGraph` — which do you prefer and why? 3) When does N+1 hurt more — in a list endpoint or a detail endpoint? 4) How would you detect N+1 in production automatically? 5) `Hibernate.initialize(...)` — what does it do and when would you use it?
**Answers:** 1) Loading a parent list triggers one query, then accessing a lazy child collection on each parent triggers N more — total 1+N SQL round trips instead of 1. 2) Both work; `@EntityGraph` on repo method is more Spring-idiomatic and composes better with pagination. `JOIN FETCH` in JPQL breaks pagination with a warning (Hibernate does in-memory paging — dangerous on large sets). 3) List endpoint — 1 workspace list × N per-workspace query multiplies. Detail endpoint at least has bounded N. 4) Integration tests with a query counter (Hypersistence Utils' `SQLStatementCountValidator`) that asserts max N queries. Or log-based alerting on request P99 query count. 5) Forces initialization of a lazy proxy while the session is still open. Useful when you must return an entity but can't restructure to a projection.

### ☐ Day 5 — Week 12 consolidation + SQL fluency + indexes

**Resource:** SQLBolt (SELECT/JOIN sections, skim) + Use The Index Luke chapters 1–2.
**Code:** on the running Postgres — `psql -U roost` or DBeaver — write raw SQL for "the 10 latest probe results per monitor" (window function or lateral join). `EXPLAIN ANALYZE` your `MonitorRepository.findByWorkspaceId(...)` query. Add `V4__idx_monitors_workspace.sql`:

```sql
CREATE INDEX idx_monitors_workspace_id ON monitors(workspace_id);
CREATE INDEX idx_probe_results_monitor_probed_at ON probe_results(monitor_id, probed_at DESC);
```

Re-EXPLAIN — you should see Index Scan instead of Seq Scan. Write `notes/12-data-layer/jpa-basics.md`.
**Self-check:** Mixed + What's the composite index rule you just applied and why?
**Answers:** Composite indexes: leftmost prefix matters. `(monitor_id, probed_at DESC)` supports "find latest results for a monitor" perfectly. Would NOT help "latest across all monitors" (needs `(probed_at, monitor_id)` or a different plan). Never index every column — write costs.

### ☐ Monday W12 — deep-work + review

**Morning (~2.5 hrs):** wire real Postgres end-to-end: `POST /monitors` persists, `GET /monitors/{id}` reads with paginated probe-result history. Fix any N+1 that shows up.
**Evening (~1.5 hrs):** recall test → `reviews/week-12-review.md` → `interview-qa/week-12-data-layer.md` (N+1 is a top-3 interview question — write 5 variants) → plan W13.

---

## WEEK 13 — Transactions + Roost core state machines

**Goal by end of week:** monitor lifecycle is transactional (create writes to `monitors` + `probe_schedule` atomically); `Incident` entity + state machine (OPEN → ACK → RESOLVED); one full user story (create monitor → probes run → incident opens → incident resolves) works end-to-end.

### ☐ Day 1 — Layered architecture audit + `Incident` entity

**Resource:** Baeldung "Layered Architecture with Spring Boot".
**Code:** audit packages: `controller/`, `service/`, `repository/`, `domain/`, `dto/`, `error/`, `config/`. Move anything misplaced. Write `V5__incidents.sql` (`id`, `monitor_id`, `status` enum, `opened_at`, `acknowledged_at`, `resolved_at`, `summary`). Create `Incident` entity + `IncidentRepository` + `IncidentService` (empty methods for now).
**Self-check:** 1) Which layer owns validation? 2) Which layer owns transactions? 3) Should the controller call the repository directly? 4) Rich vs anemic domain — what's the difference? 5) Where do enums live?
**Answers:** 1) Controller boundary (`@Valid` on `@RequestBody`); domain-invariant validation lives inside the entity/service. 2) Service. `@Transactional` on repo methods works but hides intent. 3) No — always through service; skipping bypasses transactions and business rules. 4) Rich = entities carry behavior (`monitor.markUnhealthy()`); anemic = entities are getters/setters, all logic in services. Rich is better for complex domains; anemic OK for CRUD. 5) In `domain/` — they're part of the model.

### ☐ Day 2 — `@Transactional` deep dive

**Resource:** Baeldung "Transactions With Spring and JPA".
**Code:** `MonitorService.createMonitor(...)` — write to `monitors` AND write initial `probe_schedule` row (create `V6__probe_schedule.sql`) — inside one `@Transactional` method. Force a `RuntimeException` after the first insert; verify both rows rolled back.
**Self-check:** 1) What triggers rollback by default? 2) How do you make checked exceptions roll back? 3) What is a "self-invocation" problem? 4) What does `@Transactional(readOnly = true)` actually do? 5) Where should `@Transactional` sit — controller, service, or repository?
**Answers:** 1) Runtime exceptions and Errors. Checked exceptions do NOT roll back by default. 2) `@Transactional(rollbackFor = SomeCheckedException.class)`. 3) Calling one `@Transactional` method from another _inside the same bean_ uses direct method call — bypasses the proxy — so the annotation on the inner method is ignored. Fix: move the inner call to a different bean, or use `TransactionTemplate`. 4) Signals to the tx manager it's read-only (for optimizations like skipping dirty-check on flush); doesn't prevent writes but Hibernate skips some housekeeping. 5) Service. Controller doesn't do work; repository is too fine-grained for multi-repo operations.

### ☐ Day 3 — Isolation levels & propagation

**Resource:** Baeldung "Spring Transaction Propagation and Isolation".
**Code:** demonstrate `REQUIRES_NEW` — write `AuditService.logStateChange(...)` annotated `@Transactional(propagation = REQUIRES_NEW)`. Call it from `IncidentService.resolve(...)` (which is `@Transactional(REQUIRED)`). Throw from `resolve` after the audit call. The incident update rolls back, the audit row survives. Prove it in an integration test.
**Self-check:** 1) Default propagation? 2) `REQUIRES_NEW` vs `NESTED`? 3) `READ_COMMITTED` vs `REPEATABLE_READ` vs `SERIALIZABLE` — pick one and describe. 4) Postgres default isolation? 5) What is a phantom read?
**Answers:** 1) `REQUIRED` — join existing tx or start one. 2) `REQUIRES_NEW` = suspend outer, start a truly independent tx. `NESTED` = savepoint inside outer tx; inner rollback returns to savepoint, outer can still commit — only supported by some drivers/DBs, awkward with JPA. 3) `REPEATABLE_READ`: rows you read once return the same values if you re-read them within the tx (no non-repeatable reads). 4) `READ_COMMITTED`. 5) Re-running a query returns rows that were inserted (by another tx) between the two runs — the row wasn't there before, now it's there.

### ☐ Day 4 — Pagination + filtering (real, via Spring Data)

**Resource:** Baeldung "Spring Data Pagination and Sorting" + "Spring Data JPA `Specification`".
**Code:** `GET /monitors?page=0&size=10&sort=name,asc&status=UP&type=HTTP` — accept `Pageable` and query params; write a `MonitorSpecification` (`Specification<Monitor>`) that composes optional filters. Return `Page<MonitorResponse>`.
**Self-check:** 1) `Pageable` vs `PageRequest` — relationship? 2) Why is `Specification` better than 5 method-name variants? 3) `Page<>` vs `Slice<>` — difference and when to use each? 4) How would you cap max page size? 5) What SQL runs for a `Page<>` — how many queries?
**Answers:** 1) `Pageable` is the interface; `PageRequest.of(...)` builds an instance. Spring auto-binds from `?page=&size=&sort=`. 2) Composes N optional filters without exploding method count. 3) `Page` runs a count query for total; `Slice` just checks `hasNext` — cheaper if you don't need totals. 4) `spring.data.web.pageable.max-page-size: 100` + custom `PageableHandlerMethodArgumentResolverCustomizer`. 5) Two — one for content, one for count.

### ☐ Day 5 — Week 13 consolidation: full user story

**Code:** wire `POST /monitors` (creates monitor + schedule row + audit log — all transactional) → `GET /monitors/{id}` (returns monitor + last 10 probe results — no N+1) → `POST /monitors/{id}/incidents/{iid}/acknowledge` (moves state OPEN→ACK, audit-logged in a `REQUIRES_NEW`). Test with curl and assert audit rows exist even on rollback of the parent. Note in `notes/13-transactions/`.
**Self-check:** Mixed on transactions, isolation, propagation.

### ☐ Monday W13 — deep-work + review

**Morning (~2.5 hrs):** implement `IncidentService.open()` — called (mocked for now) when a probe result is `DOWN`. Full state machine: OPEN → ACKNOWLEDGED → RESOLVED. Guard state transitions with checks inside the entity (rich domain).
**Evening (~1.5 hrs):** recall + `interview-qa/week-13-transactions.md` (5 propagation + 3 isolation Qs — top interview topics).

---

# MONTH 4 — Testing, security, internals, integrations, polish

## WEEK 14 — Testing (JUnit 5 + Mockito + slice tests + Testcontainers)

**Goal by end of week:** critical Roost paths covered — 5+ unit tests on `MonitorService`, 1 `@WebMvcTest`, 1 `@SpringBootTest` + Testcontainers integration test proving `POST /monitors` → GET round-trips through a real Postgres.

### ☐ Day 1 — JUnit 5 fundamentals

**Resource:** Baeldung "A Guide to JUnit 5" + "Parameterized Tests JUnit 5".
**Code:** pick `MonitorService.calculateUptimePercent(List<ProbeResult>)` or write it. Write 5+ tests: happy path (10 UP results = 100%), all DOWN = 0%, mixed = correct %, empty list edge (return 0 or throw — decide), one `@ParameterizedTest` with `@CsvSource` covering 4 cases.
**Self-check:** 1) `@BeforeEach` vs `@BeforeAll` — call order? 2) `assertThrows` — what does it return? 3) When to prefer AssertJ over JUnit's `assertEquals`? 4) `@Nested` — why use it? 5) How do you assert on exception messages?
**Answers:** 1) `@BeforeAll` (static) runs once per class before any tests. `@BeforeEach` runs before every test. 2) The thrown exception — useful for asserting message or cause. 3) AssertJ has fluent, chained, very readable assertions (`assertThat(list).hasSize(3).containsExactly(a, b, c)`); much better error messages. 4) Groups related tests, shows nesting in test reports, allows scoped `@BeforeEach`. 5) `assertThat(assertThrows(...)).hasMessage("...")` (AssertJ) or `assertEquals("...", ex.getMessage())`.

### ☐ Day 2 — Mockito for service unit tests

**Resource:** Baeldung "Mockito Series" (basics).
**Code:** `MonitorServiceTest` with `@ExtendWith(MockitoExtension.class)` — `@Mock MonitorRepository repo`; `@InjectMocks MonitorService service`. Test `getMonitor(1L)`: `when(repo.findById(1L)).thenReturn(Optional.of(mockMonitor))`; assert returned. Test `save`: `verify(repo).save(any(Monitor.class))`. One test where `repo.findById` returns empty — assert `MonitorNotFoundException` thrown.
**Self-check:** 1) `@Mock` vs `@Spy`? 2) `when().thenReturn()` vs `doReturn().when()` — when different? 3) `verify(mock, times(2))` — what if you don't specify? 4) `ArgumentCaptor` — when do you use it? 5) Why NOT mock the class under test's own methods?
**Answers:** 1) `@Mock` = fully fake, no real behavior. `@Spy` = wraps a real instance, stubs override only what you set. 2) `when.thenReturn` invokes the method to record — problematic with void methods or when you want to stub without executing. `doReturn.when` doesn't invoke — use for void, throwing methods, or spies. 3) Default is `times(1)`. 4) When you need to assert _what_ was passed to a mocked method (the argument was built inside the service, you can't easily reconstruct it in the test). 5) Because you're then testing your test, not your code.

### ☐ Day 3 — `@WebMvcTest` controller slice tests

**Resource:** Baeldung "Testing Controllers with `@WebMvcTest`".
**Code:** `MonitorControllerTest` annotated `@WebMvcTest(MonitorController.class)`. Inject `MockMvc`. `@MockitoBean` (SB4 replacement for `@MockBean`) the service. Test: `GET /monitors/1` returns 200 + expected JSON body (`jsonPath("$.name").value("Prod API")`); `GET /monitors/999` returns 404; `POST /monitors` with invalid body returns 400 + field errors.
**Self-check:** 1) What does `@WebMvcTest` load vs `@SpringBootTest`? 2) Why is it faster? 3) `@MockitoBean` (SB4) vs `@MockBean` (deprecated)? 4) `MockMvc` vs `WebTestClient`? 5) Why can't you test transactions with `@WebMvcTest`?
**Answers:** 1) `@WebMvcTest` = MVC infra only (controllers, `@ControllerAdvice`, filters, converters). No `@Service`, no `@Repository`, no DB. `@SpringBootTest` = whole app context. 2) No DB, no service layer wiring, boots in ~2 sec. 3) `@MockitoBean` is Spring 6+/SB3+ replacement; `@MockBean` is deprecated in newer versions. Same idea — puts a Mockito mock into the app context. 4) `MockMvc` = synchronous, servlet-based, fast, no real HTTP. `WebTestClient` = reactive style, works for MVC and WebFlux. 5) Because no DB or tx manager is loaded — for tx behavior use integration tests.

### ☐ Day 4 — `@SpringBootTest` + Testcontainers (real Postgres)

**Resource:** Baeldung "Testing with `@SpringBootTest`" + "Testcontainers with Spring Boot".
**Code:** add deps `org.testcontainers:junit-jupiter` + `org.testcontainers:postgresql`. Write `MonitorPersistenceIntegrationTest` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Container` static `PostgreSQLContainer<>("postgres:17")` + `@DynamicPropertySource` to override `spring.datasource.url`. Use `TestRestTemplate` to `POST /workspaces` then `POST /monitors` then `GET /monitors/{id}` — assert round-trip.
**Self-check:** 1) Why real DB in tests (not H2)? 2) `@Container` static vs instance — difference? 3) What is `@DynamicPropertySource`? 4) Test lifecycle: DB migrated per test class or per test method? 5) CI cost of Testcontainers?
**Answers:** 1) Prod uses Postgres. H2 lies — different SQL dialect, no JSONB, no window functions, wrong casing rules. Bugs found in prod only. 2) Static = one container for the class (fast, tests share state — clean up in `@BeforeEach`). Instance = one per test (isolated, slow). 3) Registers property values dynamically after container starts — used to inject the container's random port into `spring.datasource.url`. 4) Class — Flyway migrates once; you clean rows per test. 5) Docker required; slower than in-memory (~15 sec startup); worth it — real Postgres in CI catches real bugs.

### ☐ Day 5 — Week 14 consolidation + coverage sanity

**Code:** write 1 controller slice + 2 service unit + 1 integration test for the core write flow (`POST /monitors`). Aim for meaningful coverage of critical paths, not %. Note in `notes/14-testing/`.

### ☐ Monday W14 — deep-work + review

**Morning:** write the concurrent-probe integration test — 20 threads call `POST /probe-results` for the same monitor, assert no duplicate audit rows, no lost updates.
**Evening:** recall + `interview-qa/week-14-testing.md`.

---

## WEEK 15 — Security + JWT + **multi-tenant isolation** (Roost's flagship interview topic)

**Goal by end of week:** JWT auth + role-based access + provable tenant isolation (workspace A cannot read workspace B's monitors, enforced by Hibernate `@Filter` + integration test proving it).

### ☐ Day 1 — Spring Security basics + `SecurityFilterChain`

**Resource:** Baeldung "Spring Security Architecture".
**Code:** boot the app (Security is already on classpath) — observe default login. Write `SecurityConfig` with `@Bean SecurityFilterChain` — permit `/health/**`, `/swagger-ui/**`, `/v3/api-docs/**`, everything else `authenticated()`. Disable CSRF (JSON API). Set `SessionCreationPolicy.STATELESS`.
**Self-check:** 1) What is the filter chain? 2) Order of filters? 3) Why STATELESS for a JWT API? 4) Why disable CSRF for a JSON API? 5) `hasRole("ADMIN")` vs `hasAuthority("ROLE_ADMIN")`?
**Answers:** 1) Ordered chain of `Filter`s Spring Security wires into servlet chain. Auth, authz, session mgmt, exception handling. 2) Configurable; the first that responds handles the request. 3) No `HttpSession`, no cookies, every request self-authenticates via JWT — scales horizontally, no session store. 4) CSRF protects cookie-based auth (browser auto-sends cookies). JWT is sent explicitly in `Authorization` header — no auto-send, no CSRF risk. 5) `hasRole` prepends `ROLE_` automatically. Same thing.

### ☐ Day 2 — Users + password hashing + register/login

**Resource:** Baeldung "Registration with Spring Security" + "BCryptPasswordEncoder".
**Code:** `V7__users.sql` (id, email unique, password_hash, role); `User` entity; `UserRepository`; `UserDetailsService` implementation; `@Bean PasswordEncoder bcrypt`. `AuthController` with `POST /auth/register` (hash + store) and `POST /auth/login` (validate via `AuthenticationManager`, return 200 — JWT lands tomorrow).
**Self-check:** 1) Why hash passwords, why bcrypt specifically? 2) What's a salt and how does bcrypt handle it? 3) Login flow: what does `AuthenticationManager` do? 4) What happens if you store a hashed password as `VARCHAR(60)`? 5) Rate-limit the login endpoint — where would you do it?
**Answers:** 1) Storing plain = breach = credential leak. Bcrypt = slow, adaptive cost factor (harder as CPUs get faster), built-in salt. 2) Random per-user prefix to prevent rainbow tables. Bcrypt embeds it in the hash string. 3) Delegates to `AuthenticationProvider`s (`DaoAuthenticationProvider` uses `UserDetailsService` + `PasswordEncoder`) — returns an authenticated `Authentication` or throws. 4) Bcrypt output is exactly 60 chars — VARCHAR(60) is intentional. 5) Filter before Spring Security's chain, per-IP via Redis-backed bucket (bucket4j). W17 topic.

### ☐ Day 3 — JWT: generate + validate

**Resource:** Baeldung "Spring Security OAuth2 Resource Server" (JWT section) + jwt.io "Introduction".
**Code:** add `spring-boot-starter-oauth2-resource-server`. Configure HS256 with `spring.security.oauth2.resourceserver.jwt.secret: ${JWT_SECRET}`. Write `JwtService.issueToken(User user)` — subject = email, claim `roles`, claim `workspaceId`, expiration 15 min. Return the JWT from `/auth/login`. Decode at jwt.io — identify header, payload, signature.
**Self-check:** 1) 3 parts of a JWT? 2) HS256 vs RS256 — when do you pick each? 3) Is a JWT encrypted? 4) Can you revoke a JWT? 5) Why 15-min expiration and not 24 hours?
**Answers:** 1) Header (algo, type), payload (claims), signature (HMAC of `base64(header).base64(payload)` with secret). 2) HS256 = symmetric secret (one server or shared secret across trusted services). RS256 = asymmetric (public key can validate, private key issues — used when many services need to verify without sharing secret). 3) No — base64-encoded, human-readable. Never put secrets in a JWT. Signed, not encrypted. 4) Not directly — stateless. Common patterns: short TTL + refresh tokens; or a blocklist of revoked jti's in Redis. 5) Blast radius. Stolen 15-min token = 15 min of damage. Combine with a refresh token stored server-side (rotated on use like EventPulse v2 did).

### ☐ Day 4 — JWT validation filter + role-based authorization

**Resource:** Baeldung "Method Security with `@PreAuthorize`".
**Code:** Spring's resource-server auto-configures a JWT validation filter — configure it in `SecurityConfig` via `.oauth2ResourceServer(oauth -> oauth.jwt())`. Add `@PreAuthorize("hasRole('OWNER')")` on `POST /workspaces/{id}/members`. Confirm 403 for a MEMBER token, 200 for an OWNER token. Test with real curl using tokens from `/auth/login`.
**Self-check:** 1) `@PreAuthorize` vs `@Secured` vs `hasRole()` in filter chain? 2) Where do you fetch `roles` from — the JWT payload or the DB? 3) SpEL in `@PreAuthorize` — one useful example? 4) What happens when the token is expired? 5) How do you customize the 401/403 response body?
**Answers:** 1) `@PreAuthorize` = SpEL (flexible: `hasRole()`, `hasPermission()`, `#id == principal.id`). `@Secured` = simpler, roles only. Filter-chain rules = coarse URL patterns. Combine: filter chain for coarse gates, `@PreAuthorize` for method-level. 2) JWT payload — self-contained. If roles change server-side mid-token lifetime, they only take effect after next login. Trade-off vs DB check per request (slower). 3) `@PreAuthorize("#workspaceId == authentication.principal.workspaceId")` — object-level check. 4) `JwtException`; framework returns 401 automatically. 5) Custom `AuthenticationEntryPoint` bean.

### ☐ Day 5 — **Multi-tenant isolation via Hibernate `@Filter`** (the interview-favorite topic)

**Resource:** Baeldung "Multi-Tenancy with Hibernate" + Vlad Mihalcea "Hibernate `@Filter`".
**Code:** add `workspace_id` column to `monitors`, `incidents`, `probe_results`. Extract `workspaceId` from the JWT into `TenantContext` (a `ThreadLocal<Long>`) via a filter/interceptor. On each entity, add:

```java
@Filter(name = "tenantFilter", condition = "workspace_id = :tenantId")
```

Enable per-session: in a `HibernateInterceptor` fetch the tenant from `TenantContext`, `session.enableFilter("tenantFilter").setParameter("tenantId", tenantId)`. Write integration test — auth as user in workspace A, `GET /monitors` returns only A's monitors even if B's exist. **This test is the one you'll show recruiters.**
**Self-check:** 1) `@Filter` (row-level) vs schema-per-tenant vs DB-per-tenant — pros/cons of each? 2) Why `ThreadLocal` for tenant context? What's the danger with virtual threads? 3) Can a bug in a repo query bypass `@Filter`? 4) How do admin/cross-tenant queries work with `@Filter` enabled? 5) How would you prove isolation in a security audit?
**Answers:** 1) Row-level = one DB/schema, cheap, easy backups, but bug = data leak risk. Schema-per-tenant = strong isolation, still one DB, harder migrations (N schemas). DB-per-tenant = strongest isolation, high ops cost, limits tenant count. For a startup: row-level with `@Filter` is the standard. 2) Web requests are one-thread-per-request; ThreadLocal survives the request. With virtual threads: still one-per-request, so ThreadLocal is fine — but _never_ pass ThreadLocal into async code without propagation (`TaskDecorator` in Spring). 3) Yes if the query uses `EntityManager.createNativeQuery` directly bypassing Hibernate's filter. Mitigation: forbid raw queries in review, or add a Postgres row-level security (RLS) backstop. 4) Disable the filter in a scoped `session.disableFilter(...)` — an admin-only service method that's audited. 5) The integration test from today + a Postgres RLS policy as backstop + code review rule against raw queries.

### ☐ Monday W15 — deep-work + review

**Morning:** wire full auth end-to-end — register, login (JWT returned), authenticated `GET /workspaces/mine` returning only the caller's workspaces via `@Filter`. Prove isolation with a manual 2-user curl test.
**Evening:** `interview-qa/week-15-security-multitenant.md` — 8+ tenant-isolation Qs (this is _the_ interview topic for MNC B2B roles).

---

## WEEK 16 — Spring internals: AOP, lifecycle, caching, virtual threads

### ☐ Day 1 — AOP: `@Aspect`, `@Around`, pointcuts

**Resource:** Baeldung "Intro to Spring AOP" + "Spring AOP `@Around`".
**Code:** custom annotation `@LogExecutionTime`; aspect `LoggingAspect` with `@Around("@annotation(LogExecutionTime)")` logging method name + duration. Apply to `ProbeRunner.probe(...)` and `IncidentService.open(...)`.
**Self-check:** 1) What is a proxy in Spring AOP? 2) JDK dynamic proxy vs CGLIB — when does Spring pick which? 3) Self-invocation problem in AOP — same as `@Transactional`? 4) `@Around` vs `@Before` — when do you need `@Around`? 5) Common AOP use cases beyond logging?
**Answers:** 1) A generated wrapper class that intercepts method calls, runs advice, then delegates to the real target. 2) JDK proxy = when target implements an interface. CGLIB = subclass-based, when no interface (final classes and methods can't be proxied). 3) Yes — same reason. Direct method call bypasses the proxy. 4) When you need to control invocation (modify args, skip call, retry, catch exceptions and return alternative). `@Before` only observes. 5) Transactions, security (`@PreAuthorize`), caching, metrics, retries (`@Retryable`), rate limiting.

### ☐ Day 2 — Bean lifecycle + scopes

**Resource:** Baeldung "Spring Bean Lifecycle" + "Spring Bean Scopes".
**Code:** `ProbeScheduler` with `@PostConstruct` (log "scheduler ready" + populate initial state) and `@PreDestroy` (log "scheduler shutting down"). Create a `@Scope("prototype")` bean and prove that injecting into a singleton captures ONE instance — fix with `ObjectProvider<T>` or `@Lookup`.
**Self-check:** 1) Bean scopes in Spring — name 3? 2) When is `@PostConstruct` called? 3) `@PostConstruct` vs `InitializingBean.afterPropertiesSet()`? 4) When would you use prototype scope? 5) Request/session scope — used when?
**Answers:** 1) singleton (default), prototype, request, session, application, websocket. 2) After DI is complete, before the bean is put into service. 3) Functionally similar; annotation-based is preferred. Interface = older/framework-coupled. 4) When each user of the bean needs a fresh instance (stateful strategy objects, non-thread-safe collaborators). 5) Web-scoped beans that live for one HTTP request or one session — rare in modern REST APIs (stateless preferred).

### ☐ Day 3 — Custom auto-configuration (Roost webhook module as a starter)

**Resource:** Baeldung "Create a Custom Auto-Configuration".
**Code:** carve `com.chandu.roost.webhook.*` into a "module": `WebhookProperties` (`@ConfigurationProperties("roost.webhook")`), `WebhookService`, `WebhookAutoConfiguration` with `@ConditionalOnMissingBean` + `@ConditionalOnProperty(name = "roost.webhook.enabled", havingValue = "true")`. Register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Consume from `IncidentService`.
**Self-check:** 1) How does SB discover auto-configs at startup? 2) Common `@ConditionalOn*` variants — name 4? 3) Why `@ConditionalOnMissingBean`? 4) What does `META-INF/spring/...AutoConfiguration.imports` do? 5) Order of auto-config execution?
**Answers:** 1) Reads `AutoConfiguration.imports` files from all JARs on classpath — each line is a fully-qualified class name. 2) `OnClass`, `OnMissingClass`, `OnBean`, `OnMissingBean`, `OnProperty`, `OnWebApplication`. 3) So a user of your starter can override your default by defining their own bean of the same type. Non-invasive. 4) Modern (SB3+) replacement for `spring.factories`. Explicit list of auto-config classes to import. 5) Determined by `@AutoConfigureBefore`/`@AutoConfigureAfter`/`@AutoConfigureOrder`; else undefined.

### ☐ Day 4 — Caching + Redis

**Resource:** Baeldung "Guide to Caching in Spring" + "Spring Data Redis Cache".
**Code:** `@EnableCaching`. Annotate `MonitorService.listByWorkspace(...)` with `@Cacheable("monitors")`. Start with in-memory `ConcurrentMapCacheManager`. Then add Redis via `spring-boot-starter-data-redis` + docker-compose redis. Configure `RedisCacheConfiguration` with a 60s TTL. Demonstrate `@CacheEvict` on `save`/`delete` (invalidates the cached list).
**Self-check:** 1) `@Cacheable` vs `@CachePut` vs `@CacheEvict`? 2) Cache key — how is it generated by default? 3) Cache-aside vs read-through vs write-through — which does Spring do? 4) Cache stampede — what is it, how do you prevent it? 5) Why TTL — what happens without one?
**Answers:** 1) `@Cacheable` = read; return cached or compute+store. `@CachePut` = always compute, always update. `@CacheEvict` = remove. 2) `SimpleKeyGenerator` — hash of args. Override with `key = "#workspaceId"`. 3) Cache-aside — app checks cache, misses go to DB, then app writes cache. 4) 100 concurrent misses = 100 concurrent DB hits + 100 cache writes. Prevent with per-key locks (`@Cacheable(sync = true)`) or short "loading" placeholder. 5) Without TTL = stale forever unless explicitly evicted. Bugs multiply.

### ☐ Day 5 — `@Async`, `@Scheduled`, **virtual threads** for probes

**Resource:** Baeldung "Spring `@Async`" + "Spring `@Scheduled`" + Spring blog "Virtual Threads in Spring Boot 3.2+".
**Code:** `@EnableAsync` + `@EnableScheduling`. `ProbeScheduler` with `@Scheduled(fixedDelay = 10000)` that fetches due monitors and dispatches each probe. In `application.yml`: `spring.threads.virtual.enabled: true` — controllers now use virtual threads. Add `@Async` on the actual probe call — it uses the virtual-thread executor. Log `Thread.currentThread()` to prove.
**Self-check:** 1) `fixedRate` vs `fixedDelay` vs `cron`? 2) Why virtual threads for probe workload? 3) `@Async` return type — `void` vs `CompletableFuture<T>`? 4) What's the default `@Async` executor? Why is that a pitfall? 5) Virtual threads: what problem does pinning cause?
**Answers:** 1) `fixedRate` = time between _starts_; overlaps if slow. `fixedDelay` = time between end-of-N and start-of-N+1; never overlaps. `cron` = specific schedule ("0 0 \* \* \* \*"). 2) Probes are I/O-bound (HTTP calls) — virtual threads scale to millions cheaply. Traditional pool: 200 threads = 200 concurrent probes; virtual: 10k+ trivially. 3) `void` = fire-and-forget, exceptions lost. `CompletableFuture<T>` = can chain, catch exceptions, join. Prefer the latter unless truly fire-and-forget. 4) `SimpleAsyncTaskExecutor` — no pool, spawns unbounded threads per call. Configure a named `ThreadPoolTaskExecutor` bean and route via `@Async("customExecutor")`. Or, since Java 21, use `spring.task.execution.thread-name-prefix` + virtual-thread executor. 5) Native code, `synchronized` blocks, and `Thread.currentThread()`-holding operations pin the virtual thread to the carrier — blocks a platform thread — defeats the purpose. Use `ReentrantLock` instead of `synchronized`.

### ☐ Monday W16 — deep-work + review

**Morning:** implement the probe engine — `@Scheduled` fetches due monitors → dispatches to virtual-thread executor → each thread hits the URL, records a `ProbeResult`, triggers incident open/close if state changed. This is the _heart_ of Roost.
**Evening:** `interview-qa/week-16-internals.md` — virtual threads is a hot topic.

---

## WEEK 17 — Integrations: outbox + Kafka + webhooks + auditing

### ☐ Day 1 — Calling external APIs (`RestClient`, `WebClient`, `@HttpExchange`)

**Resource:** Baeldung "Spring RestClient" + "Spring `@HttpExchange`".
**Code:** replace the probe's `HttpProbeClient` stub with a real HTTP call using `RestClient` (Spring 6+). Timeout = probe's configured timeoutMillis. Handle 2xx/3xx = UP, 4xx/5xx = DOWN, connection error = DOWN with reason.
**Self-check:** 1) `RestTemplate` (legacy) vs `RestClient` (Spring 6+) vs `WebClient` — which for what? 2) Timeouts on `RestClient` — how? 3) Retry — where does it belong (client, wrapper, aspect)? 4) Connection pool — do you need one? 5) How do you propagate a request-id through outbound calls (tracing)?
**Answers:** 1) `RestTemplate` = deprecated maintenance mode. `RestClient` = modern synchronous, replaces RestTemplate. `WebClient` = reactive; use in reactive stack or when you want backpressure. 2) Underlying HTTP client — for JDK's `HttpClient`, set connect+read timeouts via builder. 3) Wrapper method (retry policy is business logic, not transport). Or Spring Retry `@Retryable`. Never inside the transport client — hides behavior. 4) Yes — reusing connections avoids TCP+TLS handshake per call. JDK HttpClient pools by default. 5) MDC + interceptor that copies MDC value into `X-Request-Id` header.

### ☐ Day 2 — Outbox pattern (reliable events)

**Resource:** Chris Richardson "Transactional Outbox Pattern" (microservices.io) + Baeldung "Transactional Outbox".
**Code:** `V8__outbox.sql` (id, aggregate*type, aggregate_id, event_type, payload jsonb, created_at, published_at nullable). When `IncidentService.open` runs inside its transaction, it writes: (a) new `Incident`, (b) audit row, (c) outbox row `event_type = INCIDENT_OPENED`. Same tx. Later, a `@Scheduled` publisher polls unpublished outbox rows and sends them (to Kafka tomorrow).
**Self-check:** 1) What problem does the outbox solve? 2) Why not just call the message broker from the service directly? 3) At-least-once vs exactly-once — which does outbox give you? 4) How do you handle poison messages? 5) Ordering guarantees?
**Answers:** 1) Dual-write problem: writing to DB + broker in two separate calls; if the second fails, one side is out of sync. Outbox makes both writes atomic (same DB tx). 2) Because broker send is a network call \_outside* the DB tx — if it fails after commit or succeeds after rollback, state diverges. 3) At-least-once — publisher might publish, crash before marking `published_at`, restart, republish. Consumers must be idempotent. 4) Retry with backoff; after N failures, mark row `poison` and alert. 5) Within one aggregate, DB insert order = publish order (single publisher, ordered scan). Across aggregates: no ordering unless partitioned by aggregate key on Kafka.

### ☐ Day 3 — Kafka: producer + consumer for `INCIDENT_OPENED`

**Resource:** Baeldung "Kafka with Spring Boot".
**Code:** add `spring-kafka` + docker-compose Kafka (bitnami/kafka). Configure producer + topic `roost.incidents`. Rewrite yesterday's outbox publisher to send to Kafka. Add a `NotificationConsumer` `@KafkaListener` on the topic that logs `"would send email to on-call for incident X"` — real notification wiring is future work.
**Self-check:** 1) Topic vs partition — how do they affect ordering and throughput? 2) Consumer group — one consumer or many? 3) Acks: 0 vs 1 vs all — trade-offs? 4) Rebalancing — what triggers it, why it matters? 5) Dead-letter topic — when and why?
**Answers:** 1) Topic = logical stream; partitions = ordered logs, keys hash to partitions. Ordering guaranteed _per partition_, not across topic. Throughput scales with partitions. 2) Consumer group = load-balanced set. One consumer per partition max within group. Multiple groups = each gets a full copy of the stream. 3) `acks=0` fire-and-forget (fastest, unsafe). `acks=1` leader acks (default; loses on leader failure). `acks=all` all in-sync replicas ack (safest, slowest). 4) Consumer joins/leaves, partition reassignment. Causes brief pause + duplicate reads. 5) After N failed processing attempts, move to DLQ topic; process out-of-band. Prevents one bad message stalling the consumer.

### ☐ Day 4 — Auditing + optimistic locking (`@Version`)

**Resource:** Baeldung "JPA Auditing" + "JPA `@Version`".
**Code:** `@EnableJpaAuditing`. `Auditable` base class with `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`; `AuditorAware<String>` bean returning current user from `SecurityContextHolder`. Add `@Version Long version` to `Incident`. In an integration test, simulate two concurrent updates to the same incident — assert one succeeds and the other throws `ObjectOptimisticLockingFailureException`.
**Self-check:** 1) Optimistic vs pessimistic locking — pick each for a use case. 2) How does `@Version` work at SQL level? 3) What HTTP status should you return to the client on a version conflict? 4) `AuditorAware` — why not just read `SecurityContext` directly in `@PrePersist`? 5) Immutable audit trail — how do you achieve it here?
**Answers:** 1) Optimistic: no locks held; conflict detected at commit (cheap, works well when contention low). Pessimistic: `SELECT FOR UPDATE` holds the row (used when contention high, e.g., seat inventory in EventPulse). 2) UPDATE ... WHERE id=? AND version=?; if no rows updated → conflict. Hibernate throws. 3) 409 Conflict, with a message saying "reload and retry." 4) Cleaner separation — the audit machinery is decoupled from SecurityContext; testable independently by swapping the `AuditorAware` bean. 5) The outbox rows + audit trail table serve as immutable log; DB permission for the app user is INSERT-only on `audit_log`.

### ☐ Day 5 — Outbound webhooks with HMAC signature verification

**Resource:** Stripe docs "Webhook signature verification" (concepts) + Baeldung "HMAC SHA256 in Java".
**Code:** on `INCIDENT_OPENED` Kafka event, `WebhookService` looks up subscribers for that tenant, POSTs the event to each subscriber's URL, signs body with HMAC-SHA256 of `secret + timestamp + body`, sends signature as `X-Roost-Signature` header. Retry with exponential backoff on 5xx; give up after 5 attempts.
**Self-check:** 1) Why HMAC and not just an API key? 2) Why include timestamp in the signature? 3) Replay attacks — how does timestamp help? 4) Backoff strategy — pick one and defend. 5) Return code from _your_ webhook endpoint — what do you accept?
**Answers:** 1) API key = static credential; if leaked, attacker impersonates permanently. HMAC = proves this message body was signed with the shared secret; can't be replayed with a different body. 2) Prevents replay: recipient rejects requests older than 5 min. 3) Attacker captures a legitimate call, replays later — signature valid but timestamp stale → reject. 4) Exponential backoff with jitter: base × 2^n + random. Prevents thundering-herd retries. Cap total retry time. 5) 2xx = success. 4xx = don't retry (they say the message was bad). 5xx = retry.

### ☐ Monday W17 — deep-work + review

**Morning:** wire the full incident pipeline: probe fails → `ProbeResult` DOWN persisted → `IncidentService.openIfNew` inside tx writes outbox row → outbox polling publisher sends to Kafka → `NotificationConsumer` picks it up → `WebhookService` fans out to subscribers with signed body. All async, all at-least-once, all traceable.
**Evening:** `interview-qa/week-17-integrations.md`.

---

## WEEK 18 — Public status pages + observability

### ☐ Day 1 — Public status page per tenant (subdomain routing)

**Code:** in `SecurityConfig` permit `/public/status/**`. Write a `TenantResolutionFilter` that inspects `Host` header — if `<slug>.roost.local:8080`, resolve to workspace by slug, set `TenantContext`. `PublicStatusController` at `/public/status`: current status of every published monitor + last 5 incidents. Test locally: add `127.0.0.1 acme.roost.local demo.roost.local` to `hosts` file, curl `http://acme.roost.local:8080/public/status`.
**Self-check:** 1) Why subdomain instead of `/status/{slug}`? 2) Cookie/CSRF implications of subdomains? 3) How would this scale — 10k tenants, DNS strategy? 4) Public endpoints and multi-tenant filter — how do they coexist? 5) What's the risk if you forget to disable tenant filter for a public endpoint? Or if you forget to check `published = true`?
**Answers:** 1) Branding, cookie scoping if you add web auth later, easier CDN caching per tenant, no path-based tenant ID leak in URLs. 2) `*.roost.local` cookies scope to subdomains automatically; be careful not to set root-domain cookies accidentally. 3) Wildcard DNS `*.roost.dev A <load-balancer-ip>`, SSL via wildcard cert or ACME per-tenant. 4) Public endpoints resolve tenant from `Host` header, then enable the filter with that tenant — same filter, different resolution path. 5) You leak tenant A's monitors to tenant B's status page. Integration test explicitly for this: put a monitor in A, hit B's status page, assert it's absent.

### ☐ Day 2 — Actuator + Micrometer + Prometheus

**Resource:** Baeldung "Spring Boot Actuator" + "Micrometer + Prometheus".
**Code:** expose `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`. Add `micrometer-registry-prometheus`. Custom metrics: `roost.probes.executed` counter, `roost.probes.duration` timer, `roost.incidents.open` gauge. Curl `/actuator/prometheus` — see them exposed in the Prometheus text format.
**Self-check:** 1) What data does `/actuator/health` include and how do you customize it? 2) Counter vs Gauge vs Timer — pick each for a metric. 3) Why not expose all actuator endpoints in prod? 4) Prometheus scrape model — pull vs push? 5) How would you avoid label explosion (high-cardinality tags)?
**Answers:** 1) Basic UP/DOWN + individual `HealthIndicator` beans (DB, Redis, Kafka). Custom: implement `HealthIndicator`. 2) Counter (monotonic increasing — probes executed). Gauge (point-in-time — open incidents). Timer (histogram of durations — probe latency). 3) Sensitive data (env vars, heapdump, threaddump). Expose selectively, secure with security. 4) Pull — Prometheus scrapes `/actuator/prometheus` on schedule. Push (Prometheus Pushgateway) only for batch jobs. 5) Never tag by user_id / URL / any high-cardinality attribute. Bucket into categorical values.

### ☐ Day 3 — Structured logging + correlation IDs

**Resource:** Baeldung "Structured Logging with Logback".
**Code:** switch Logback to JSON encoder (`logstash-logback-encoder`). Add `RequestIdFilter` that reads `X-Request-Id` header (or generates a UUID) and puts it in MDC. Ensure every log line includes it. Prove it in curl: `curl -H 'X-Request-Id: test-123' http://localhost:8080/monitors` — log lines carry `"requestId": "test-123"`.
**Self-check:** 1) Why JSON logs in prod? 2) MDC — what is it, when is it cleared? 3) Correlation ID across async — pitfall? 4) Sensitive data in logs — how do you strip? 5) Log levels — what should DEBUG/INFO/WARN/ERROR each contain?
**Answers:** 1) Aggregators (ELK, Loki, Datadog) parse fields for search/aggregation. Plain text = grep only. 2) Mapped Diagnostic Context — per-thread map that logging appenders read. Cleared at request end (in the filter's `finally`). 3) `@Async` = new thread = empty MDC. Fix with a `TaskDecorator` that copies MDC to the async thread. 4) Never log raw request bodies. Use a filter that redacts specific fields (e.g., `password`). 5) DEBUG = fine-grained flow. INFO = business events. WARN = recoverable oddities. ERROR = failures needing attention.

### ☐ Day 4 — GitHub Actions CI

**Code:** `.github/workflows/ci.yml` — triggers on push + PR, JDK 25 setup (matches pom `java.version`), cache Maven repo, `./mvnw -B verify`, status badge in README. Break a test intentionally on a branch, confirm check goes red.
**Self-check:** 1) Why `-B` (`--batch-mode`)? 2) Testcontainers in CI — is Docker available? 3) Fail-fast for the build — pros/cons? 4) Should you run integration tests on every PR or nightly? 5) Secrets in GitHub Actions — how?
**Answers:** 1) Non-interactive; suppresses download progress spam in logs. 2) On GitHub-hosted runners: yes, Docker is preinstalled. On self-hosted: verify. 3) Fail-fast stops the pipeline on first failure — saves CI minutes, but you lose visibility into whether other stages would have failed too. Trade-off. 4) Small: every PR. Large (slow integration suites): every PR for critical paths, full nightly. 5) `secrets.NAME` — encrypted, injected as env vars or step inputs.

### ☐ Day 5 — `docker-compose.yml` for full local stack + handoff to `system-design/`

**Code:** production-shaped `docker-compose.yml` at repo root — services: `db` (postgres:17), `redis`, `kafka` (bitnami/kafka), `app` (multi-stage Dockerfile). App image built from `./Dockerfile`. `docker compose up` and everything comes up. **This is the handoff to system-design/ Week 20**, which takes this and deploys to AWS.
**Self-check:** 1) Multi-stage Dockerfile — why? 2) `.dockerignore` — what belongs? 3) `wait-for-it.sh` vs `depends_on` — difference? 4) One-container-per-process — why? 5) Prod Dockerfile vs dev Dockerfile — differ how?
**Answers:** 1) Build stage has JDK + Maven (~500MB); runtime stage has only JRE + jar (~200MB). Smaller image = faster deploy. 2) `target/`, `.git/`, `.idea/`, `*.md` — don't waste build context. 3) `depends_on` = start order, not "ready" order. Use healthchecks or app-level retries for actual readiness. 4) Simpler restart/scale; one crash doesn't take down everything; observability per container. 5) Prod: no DevTools, no hot reload, hardened base image, non-root user. Dev: reload on change.

### ☐ Monday W18 — deep-work + review

**Morning:** end-to-end demo — spin down everything, `docker compose up`, verify probes run, incidents open, notifications fire, `/actuator/prometheus` scrapes clean, public status page renders per tenant.
**Evening:** `interview-qa/week-18-observability.md`.

---

## WEEK 19 — Polish + Phase B revision + handoff-ready

### ☐ Day 1 — Feature completion pass

Finish any missing user stories. Every documented endpoint returns validated, transactional, tested responses.

### ☐ Day 2 — Error handling audit + logging polish

Audit every controller — no raw stack trace can escape to the client. Every 4xx returns a `ProblemDetail`. Every 5xx logs ERROR with full context. Redact sensitive data.

### ☐ Day 3 — README + architecture diagram + design doc

Rewrite README: what/why/stack/live-demo/architecture (Mermaid)/local run/API examples/roadmap/license. Write `docs/DESIGN.md` — the interesting problems and how you solved them (mirror the EventPulse v2 README shape).

### ☐ Day 4 — Refactor + code review pass

Run yourself as reviewer. Dead code out. TODOs resolved or ticketed. Format. Kill obvious duplication.

### ☐ Day 5 — Phase B recall test (closed book) + Phase B consolidated interview-QA

Recall test on Weeks 10–19. Confidence per week. Generate `interview-qa/phase-b-consolidated-qa.md` — top ~50 Spring Boot questions grounded in what you built (not googled generic lists).

### ☐ Monday W19 — Phase B monthly checkpoint

**Morning:** any leftover polish + demo dry-run.
**Evening:** monthly checkpoint — measure pace vs Mar 2027 target. Confirm handoff-ready to `system-design/` Week 20 (Docker → EC2 → RDS → ALB → ECS).

---

## Books & backup resources

- _Effective Java_ (Bloch) — read item by item during idle moments
- _Spring Start Here_ (Spilcă) — for topics that don't click
- Chad Darby "Spring Boot 3 Master Class" (Udemy) — course alternative to Amigoscode
- Telusko / Bro Code / Java Brains — video alternatives per topic
- Baeldung — the reference index for any Spring topic

## Cross-repo pointers

- **Java-Springboot/** — core Java archive (Weeks 1–9). Read notes only when a topic revisits.
- **system-design/** — Month 5 (Weeks 20–23) picks up Roost's `docker-compose.yml` and Dockerfile, deploys to AWS.
- **AI/** — Month 7 adds AI incident postmortems + semantic monitor grouping.
- **DSA/** — daily DSA mini-sessions run in a separate chat, ~25 min after this session.
