# W17 Monday — the full incident pipeline, end to end

## Definition & Why
This is the heart of Roost: turning a failed HTTP check into a notification the on-call team actually receives, reliably, without blocking the thing that detected it. The pieces built across W16–W17 (scheduler, probe, incident state machine, outbox, Kafka, webhooks) only earn their keep once they're one continuous flow.

## The flow
```
@Scheduled discoverDueSchedules  (claims due monitors, one row at a time via UPDATE…WHERE)
        │  dispatch per monitor
        ▼
ProbeTask.execute  @Async (virtual thread) @Transactional
        │  HTTP probe → UP / DOWN
        ├── save ProbeResult
        ├── DOWN → IncidentService.openIfDown  (dedupes: one open incident per monitor)
        │           └── Incident + AuditLog + OutboxEvent(INCIDENT_OPENED)   ── all one commit ──┐
        └── UP   → IncidentService.resolveOnRecovery                                             │
                    └── Incident resolved + AuditLog + OutboxEvent(INCIDENT_RESOLVED)            │
                                                                                                 ▼
OutboxPublisher  @Scheduled(5s) — polls unpublished rows → Kafka roost.incidents → markPublished
        ▼
NotificationConsumer  @KafkaListener — parse envelope → WebhookService.deliver(workspaceId, body)
        ▼
WebhookService — HMAC-sign, POST to each tenant subscriber, retry with backoff
```

## Why it's built this way
- **Detection is transactional, reaction is not.** The probe result, the incident, its audit row and the outbox event commit together — there is no window where an incident exists but its event was lost, or vice versa. Everything after the outbox is asynchronous and retryable.
- **Dedup at the source.** `openIfDown` checks for an existing OPEN/ACK incident, so a monitor down for an hour yields *one* incident, not one per 10-second probe.
- **Auto-resolve on recovery** uses `resolveOnRecovery()` (OPEN/ACK → RESOLVED) rather than the manual `resolve()` (which demands an ack) — a service that heals itself shouldn't wait for a human.
- **At-least-once + idempotent consumer.** The outbox relay can double-send on a crash; `eventId` in the envelope lets consumers dedupe.
- **No blocking the detector.** The probe commits and moves on in milliseconds; Kafka, retries and slow subscriber endpoints never hold up probing.

## Interview Points
- This is a textbook **transactional outbox + async fan-out** — the standard answer to "how do you reliably send an event when a DB row changes?"
- The scheduler claims work with an atomic `UPDATE … WHERE next_probe_at <= now` so two instances never probe the same monitor twice (safe to run >1 app instance).
- Virtual threads make thousands of concurrent I/O-bound probes cheap.
- Failure isolation: a dead webhook endpoint degrades one subscriber's deliveries, nothing else.

## Common Mistakes
- Opening an incident on every DOWN probe (no dedup) → incident spam.
- Publishing to Kafka inside the business transaction → dual-write problem returns.
- Forgetting the consumer must be idempotent under at-least-once delivery.
- Auto-resolving through the manual ACK-gated path → recoveries silently fail with IllegalStateException.

## Quick Revision + Q&A
**Q: Walk me through what happens when a monitored URL goes down.**
A: The scheduler claims the due monitor, an async probe runs on a virtual thread, gets a non-2xx/timeout, saves a DOWN ProbeResult and — in the same transaction — opens an incident (if none active), writes an audit row and an outbox event. A polling relay later ships that event to Kafka; a consumer picks it up and fans out HMAC-signed webhooks to the tenant's subscribers with retry.

**Q: What guarantees the notification isn't lost if Kafka is down when the incident opens?**
A: The event is a committed DB row (outbox). The relay only marks it published after the broker acks, so it keeps retrying until Kafka is back.

**Q: Could two app instances double-probe or double-open?**
A: No. The scheduler claims each schedule with a conditional UPDATE, and `openIfDown` dedupes on an existing active incident.
