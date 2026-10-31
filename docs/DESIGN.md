# Roost — Design Notes

The interesting problems in Roost and how they're solved. This is the document to read before an interview about the project.

## 1. Reliable notifications without the dual-write problem

**Problem.** When an incident opens we must (a) persist it and (b) notify subscribers. Doing both as separate calls (DB write, then broker publish) is a dual-write: if the publish fails after the commit — or succeeds after a rollback — the two systems disagree, and a real outage goes unnotified.

**Solution.** The **transactional outbox**. `IncidentService.open` writes the incident, its audit row, *and* an `outbox_events` row in a single transaction. A separate `@Scheduled` `OutboxPublisher` polls unpublished rows, ships them to Kafka, and only then sets `published_at`. Delivery is **at-least-once**: a crash between publish and mark re-sends on the next poll, so consumers are idempotent (the envelope carries `eventId`). Publishing stops on the first failure to preserve per-aggregate order.

**Trade-off.** At-least-once (not exactly-once) and a few seconds of polling latency, in exchange for never losing an event. Worth it for alerting.

## 2. Multi-tenant isolation

**Problem.** One database holds every tenant's monitors, incidents and results. A single buggy query could leak tenant A's data to tenant B — the cardinal sin for a B2B product.

**Solution.** Defense in depth:
- Every tenant-owned table has a `workspace_id`; a Hibernate `@Filter` adds `workspace_id = :tenantId` to queries, enabled per request from the authenticated principal.
- Service methods resolve the workspace from the **JWT**, never from client input (`request.workspaceId()` is deliberately ignored for ownership).
- The public status page resolves the tenant from the **Host subdomain** and still scopes every query explicitly by `workspace_id` *and* `published = true`.

**Trade-off.** Row-level (vs schema- or DB-per-tenant) is cheapest to operate and backs up as one unit, at the cost of relying on the filter being correctly applied — mitigated by explicit scoping and tests.

## 3. Scaling the probe engine

**Problem.** Probing thousands of endpoints means thousands of concurrent, I/O-bound HTTP calls. A classic thread pool caps at a few hundred.

**Solution.** The scheduler claims each due monitor with an atomic conditional `UPDATE … WHERE next_probe_at <= now` (so multiple app instances never double-probe), then dispatches each probe as an `@Async` task on **virtual threads** (`spring.threads.virtual.enabled`). Virtual threads make tens of thousands of blocking HTTP calls cheap.

**Trade-off.** Must avoid pinning (use `ReentrantLock`, not `synchronized`, around blocking sections). Probes are plain HTTP so pinning isn't a concern here.

## 4. The incident state machine

`OPEN → ACKNOWLEDGED → RESOLVED`, enforced inside the `Incident` entity (rich domain — illegal transitions throw). Two resolve paths:
- `resolve()` — manual, requires `ACK` first (a human took ownership).
- `resolveOnRecovery()` — automatic, `OPEN`/`ACK` → `RESOLVED` when the monitor comes back UP, because a self-healing service shouldn't wait for an ack.

`openIfDown` dedupes: one active incident per monitor, however long it stays down. Concurrency on an incident is guarded by `@Version` optimistic locking → HTTP 409 on conflict.

## 5. Webhook security

Outbound webhooks are signed with **HMAC-SHA256** over `timestamp + "." + body`, sent as `X-Roost-Signature`. Signing the timestamp enables replay rejection; signing the body proves integrity. The exact received bytes are forwarded so the subscriber's recomputed signature matches. Failed deliveries retry with exponential backoff + jitter; 4xx = give up, 5xx/connection = retry.

## 6. Operability

- **Metrics** — Micrometer → Prometheus: probe counter (tagged up/down), probe-duration timer, open-incidents gauge. Low cardinality by design.
- **Logs** — JSON in prod with a `requestId` correlation id in the MDC, propagated across `@Async` via a `TaskDecorator`.
- **Errors** — RFC 7807 `ProblemDetail` everywhere; 4xx logged at INFO, 5xx at ERROR with full stack, never leaked to clients.
- **Schema** — Flyway from day one; `ddl-auto: validate` so Hibernate never mutates the schema.

## Known limitations / future work

- Poison outbox/Kafka messages are logged and skipped; a real dead-letter topic is future work.
- Webhook delivery retries inline on the consumer thread; a dedicated delivery queue would isolate slow subscribers further.
- JWTs are stateless with a 15-min TTL; refresh-token rotation is noted but not implemented here.
