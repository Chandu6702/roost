# Week 17 — Integrations (RestClient · Outbox · Kafka · Auditing · Webhooks)

Grounded in Roost's incident pipeline. These are the questions an interviewer asks about reliable eventing and external integrations.

## RestClient / outbound HTTP
**Q1. RestTemplate vs RestClient vs WebClient?**
RestTemplate is legacy (maintenance only). RestClient is the modern synchronous client (Spring 6+) — what Roost's probe and webhook sender use. WebClient is reactive; reach for it in a reactive stack or when you need backpressure.

**Q2. How do you set timeouts?**
On the underlying request factory. Roost uses `JdkClientHttpRequestFactory` over a JDK `HttpClient` with connect + read timeouts from config. A probe without a read timeout can hang a thread forever.

**Q3. Where does retry belong?**
In a wrapper around the client (business policy), or Spring Retry `@Retryable` — never buried in the transport, which hides behavior.

## Transactional outbox
**Q4. What problem does the outbox solve?**
The dual-write problem: writing to the DB and a broker in two separate calls can leave them inconsistent if the second fails. The outbox writes the event as a row in the *same* DB transaction as the state change; a relay ships it later.

**Q5. At-least-once or exactly-once?**
At-least-once. The relay can publish, crash before marking `published_at`, restart and republish. Consumers must be idempotent (we key on `eventId`).

**Q6. Why not just call Kafka from the service directly?**
The broker send is a network call outside the DB transaction — success-after-rollback or failure-after-commit diverges state.

## Kafka
**Q7. Topic vs partition — ordering and throughput?**
A topic is a logical stream; partitions are ordered logs. Ordering is guaranteed per-partition only. Record keys hash to partitions, so keying by aggregate buys per-aggregate ordering. Throughput scales with partitions.

**Q8. Consumer groups?**
A group load-balances partitions across its consumers — at most one consumer per partition within a group. A second group gets its own full copy of the stream.

**Q9. acks 0/1/all?**
0 = fire-and-forget (fastest, unsafe); 1 = leader ack (default, can lose on leader failure); all = all in-sync replicas (safest). Roost uses `all` — it's already async so the latency is invisible.

**Q10. Rebalancing?**
Triggered when a consumer joins/leaves. Causes a brief pause and possible duplicate reads — another reason to be idempotent.

**Q11. Poison message?**
After N failed attempts, route to a dead-letter topic and alert, so one bad record can't stall the partition.

## Auditing + optimistic locking
**Q12. Optimistic vs pessimistic locking?**
Optimistic: no locks, conflict detected at commit via `@Version` (low contention). Pessimistic: `SELECT … FOR UPDATE` holds the row (high contention, e.g. inventory).

**Q13. `@Version` at the SQL level?**
`UPDATE … SET …, version = version + 1 WHERE id = ? AND version = ?`. Zero rows updated ⇒ someone else won ⇒ `ObjectOptimisticLockingFailureException` ⇒ HTTP 409.

**Q14. Why `AuditorAware` instead of reading SecurityContext in `@PrePersist`?**
Decouples auditing from security, stays testable, and works on threads with no security context (background probes → "system").

## Webhooks / HMAC
**Q15. HMAC vs API key?**
An API key is a static bearer credential — leaked once, impersonated forever. HMAC proves *this exact body* was signed with the shared secret; it can't be replayed with a different body.

**Q16. Why sign the timestamp too?**
Replay protection — the receiver rejects deliveries older than a few minutes, and the timestamp is inside the signed material so it can't be forged.

**Q17. Backoff strategy?**
Exponential (`500ms·2^n`) + jitter, capped at max attempts. Exponential gives a struggling receiver room; jitter de-synchronizes retries across many events. 4xx = give up, 5xx/connection = retry.

## Pipeline (the whole thing)
**Q18. Describe Roost's path from a failed check to a delivered notification.**
Scheduler claims the due monitor → async probe on a virtual thread → DOWN result + incident (deduped) + audit + outbox event, one commit → outbox relay → Kafka → consumer → HMAC-signed webhook fan-out with retry. Detection is transactional; everything after the outbox is async and retryable.

**Q19. How do you avoid double-probing / double-opening across instances?**
Scheduler claims each schedule with an atomic conditional UPDATE; `openIfDown` dedupes on an existing active incident.
