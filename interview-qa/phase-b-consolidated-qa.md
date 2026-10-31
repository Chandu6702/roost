# Phase B — Consolidated Interview Q&A (Weeks 10–19)

The ~50 questions most likely to come up, grounded in what Roost actually does. Answer out loud; if you can't, re-read the day's note.

## Spring core & config (W10)
1. **IoC in one line?** The framework constructs and wires your objects; you don't `new` your collaborators, they're injected.
2. **DI is a form of IoC — of what?** Of dependency management — the container decides who gets what.
3. **Constructor vs field injection?** Constructor: final, immutable, testable, compiler-enforced. Roost uses it everywhere.
4. **`@Value` vs `@ConfigurationProperties`?** `@Value` for one or two props; `@ConfigurationProperties` for a validated, bound group (e.g. `WebhookProperties`, `ProbeProperties`).
5. **What does `@SpringBootApplication` combine?** `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`.
6. **Flyway vs `ddl-auto=update`?** Flyway = versioned, ordered, reviewable migrations; `validate` lets Hibernate only check, never mutate.

## REST layer (W11)
7. **`@PathVariable` vs `@RequestParam` vs `@RequestBody`?** URL segment / query string / deserialized JSON body.
8. **201 vs 204 vs 404?** Created / No Content (delete) / Not Found.
9. **Why DTOs over entities at the boundary?** Decouples API contract from DB schema; avoids leaking internal fields and lazy-loading JSON loops.
10. **Why records for DTOs?** Immutable, free equals/hashCode/toString, concise.
11. **`@NotNull` vs `@NotEmpty` vs `@NotBlank`?** not-null / not-null-and-not-empty / string-with-non-whitespace.
12. **Where does validation go?** At the boundary (`@Valid` on the DTO); domain invariants live in the entity/service.
13. **What is RFC 7807 / `ProblemDetail`?** A standard error JSON shape; Roost returns it for every error from a `@RestControllerAdvice`.
14. **Log a 4xx or a 5xx?** 4xx at INFO (client's fault), 5xx at ERROR with stack (ours).

## Data layer (W12)
15. **Owning side of `@ManyToOne`/`@OneToMany`?** The `@ManyToOne` side holds the FK and owns it.
16. **Default fetch for each?** `@ManyToOne` = EAGER (override to LAZY always), `@OneToMany` = LAZY.
17. **`LazyInitializationException` — when?** Touching a lazy field after the transaction/persistence context closed. Fix with a projection, fetch join, or `@EntityGraph`.
18. **N+1 in one line?** One query for the parents, then one per parent for a lazy collection — 1+N round trips.
19. **`JOIN FETCH` vs `@EntityGraph`?** Both fix N+1; `@EntityGraph` composes with pagination, `JOIN FETCH` in JPQL breaks it (in-memory paging).
20. **`Page` vs `Slice`?** `Page` runs a count query for totals; `Slice` only checks `hasNext` (cheaper).
21. **Composite index rule?** Leftmost-prefix. `(monitor_id, probed_at DESC)` serves "latest results for a monitor".
22. **Why not records for JPA entities?** JPA needs a no-arg constructor + mutable fields for proxies.

## Transactions (W13)
23. **What rolls back by default?** Runtime exceptions and Errors; checked exceptions do not (`rollbackFor` to change).
24. **Self-invocation problem?** Calling a `@Transactional` method from the same bean bypasses the proxy, so the annotation is ignored.
25. **`REQUIRES_NEW` vs `NESTED`?** New independent tx (suspends outer) vs a savepoint within the outer tx.
26. **Where does `@Transactional` belong?** The service layer.
27. **Postgres default isolation?** `READ_COMMITTED`.
28. **Why is the audit row written in `REQUIRES_NEW`?** So the audit survives even if the outer business tx rolls back.

## Testing (W14)
29. **`@WebMvcTest` vs `@SpringBootTest`?** MVC slice (fast, no DB) vs whole context.
30. **`@Mock` vs `@Spy`?** Fully fake vs a real instance with selective stubs.
31. **`when/thenReturn` vs `doReturn/when`?** The latter doesn't invoke the method — use for void/throwing/spies.
32. **Why Testcontainers over H2?** Real Postgres dialect/features; H2 lies and hides prod-only bugs.
33. **`@Container` static vs instance?** One per class (fast, shared) vs one per method (isolated, slow).
34. **`@DynamicPropertySource`?** Injects the container's random port into `spring.datasource.url` after it starts.

## Security & multi-tenancy (W15)
35. **Why STATELESS for a JWT API?** No session store; every request self-authenticates; scales horizontally.
36. **Why disable CSRF here?** CSRF targets cookie auth; JWT is sent explicitly in a header.
37. **3 parts of a JWT?** Header, payload, signature. Signed, not encrypted — never put secrets in it.
38. **HS256 vs RS256?** Symmetric shared secret vs asymmetric (public verify / private sign).
39. **Why 15-min token TTL?** Small blast radius if stolen; pair with refresh tokens.
40. **`@PreAuthorize` vs filter-chain rules?** SpEL method-level checks vs coarse URL gates — combine both.
41. **Row-level vs schema- vs DB-per-tenant?** Cheapest/standard vs stronger isolation at higher ops cost.
42. **How do you prove isolation?** An integration test: data in A, assert absent from B. Optionally Postgres RLS as a backstop.

## Internals (W16)
43. **What is a Spring AOP proxy?** A generated wrapper that runs advice around the real method call.
44. **JDK proxy vs CGLIB?** Interface-based vs subclass-based (when there's no interface).
45. **`@Cacheable` vs `@CachePut` vs `@CacheEvict`?** Read-through / always-write / invalidate.
46. **Cache stampede & fix?** Many concurrent misses hammer the DB; `@Cacheable(sync=true)` or short placeholders.
47. **`fixedRate` vs `fixedDelay`?** Gap between starts (can overlap) vs between end and next start.
48. **Why virtual threads for probes?** I/O-bound work scales to tens of thousands of cheap threads.
49. **Default `@Async` executor pitfall?** `SimpleAsyncTaskExecutor` spawns unbounded threads — configure one.

## Integrations & observability (W17–W18)
50. **Outbox: which delivery guarantee?** At-least-once — mark published only after the broker acks; consumers dedupe on `eventId`.
51. **Kafka ordering guarantee?** Per-partition only; key by aggregate to order within an incident.
52. **acks=all — why?** Safest (all in-sync replicas); the pipeline is async so the latency is invisible.
53. **Optimistic vs pessimistic locking?** `@Version` conflict-at-commit (low contention) vs `SELECT FOR UPDATE` (high).
54. **HMAC vs API key for webhooks?** HMAC proves *this body* was signed; a key is a static reusable bearer.
55. **Why sign the timestamp?** Replay rejection.
56. **Counter vs Gauge vs Timer?** Monotonic (probes run) / point-in-time (open incidents) / durations (probe latency).
57. **Prometheus pull vs push?** Pull — it scrapes `/actuator/prometheus`.
58. **Cardinality explosion — avoid how?** No high-cardinality tags (ids/URLs); bounded categories only.
59. **MDC across `@Async`?** Empty on a new thread — copy it with a `TaskDecorator`.
60. **Multi-stage Dockerfile — why?** Build with JDK+Maven, ship JRE+jar only — smaller, safer, faster.

## The whole system
61. **Walk me from a down URL to a delivered alert.** Scheduler claims the monitor → async virtual-thread probe → DOWN result + incident (deduped) + audit + outbox event in one commit → outbox relay → Kafka → consumer → HMAC-signed webhook with retry. Detection transactional; reaction async and retryable.
