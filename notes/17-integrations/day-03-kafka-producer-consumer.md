# W17 D3 — Kafka: outbox relay → topic → notification consumer

## Definition & Why
Kafka is a distributed, partitioned, append-only commit log. Producers append records to a **topic**; consumers in a **consumer group** read from it, each partition handled by at most one consumer in the group. Roost uses it to decouple *detecting* an incident (fast, transactional, inside the HTTP request) from *reacting* to it (notifications, webhooks — slow, external, retryable). The outbox relay is the producer; `NotificationConsumer` is the consumer.

## Mental Model
A topic is a shelf of ordered mailboxes (partitions). The record **key** decides which mailbox — same key → same partition → ordering preserved for that key. We key by `aggregateType:aggregateId` so every event for one incident stays ordered, while different incidents spread across partitions for parallelism. Consumer groups are a team sharing the shelf: add consumers up to the partition count to scale; beyond that they sit idle.

## My Roost Code (today)
- `OutboxPublisher` now injects `KafkaTemplate<String,String>`, serializes each unpublished outbox row into an `OutboxMessage` JSON envelope, sends it keyed by aggregate, and only calls `event.markPublished()` after the broker acks (`.get(5s)`). Publishing **stops on first failure** so a later event never overtakes an earlier one; the row stays unpublished and retries next poll.
- `OutboxMessage` — the wire envelope; carries `eventId` so consumers can dedupe.
- `NotificationConsumer` — `@KafkaListener` on `roost.incidents`, group `roost-notifications`; parses the envelope, ignores non-`INCIDENT_OPENED`, logs the notification intent (webhook fan-out lands D5).
- `KafkaTopicsConfig` — declares the topic (3 partitions, 1 replica) for dev auto-create.
- `docker-compose.yml` — single-node KRaft Kafka on 9092.

## Interview Points
- **Ordering** is guaranteed *per partition*, not per topic. Keying is how you buy ordering where you need it.
- **acks**: `0` fire-and-forget, `1` leader only (default), `all` = all in-sync replicas (we use `all` — safest, we're already async so latency doesn't hurt).
- Outbox + Kafka = **at-least-once**. Marking `publishedAt` only after the ack is what makes a crash re-publish rather than lose the event. Consumers must be idempotent.
- Consumer group = load balancing; a *second* group would get its own full copy of the stream.
- **Rebalancing** on join/leave causes a brief pause and possible duplicate reads — another reason to be idempotent.

## Common Mistakes
- Marking the outbox row published *before* the send confirms → lost events if the broker is down.
- Sending inside the business transaction instead of a separate relay → back to the dual-write problem.
- Assuming topic-wide ordering. It's per-partition only.
- Letting one unparseable message stall the partition — route to a dead-letter topic instead (we log-and-skip for now).
- `missing-topics-fatal: true` (the default is false in our config) can stop the app booting if the broker isn't ready.

## Quick Revision + Q&A
**Q: Why not have the service call Kafka directly on incident open?**
A: Broker send is a network call outside the DB transaction. If it succeeds after a rollback, or fails after commit, state diverges — the dual-write problem. The outbox writes the event in the same commit; the relay ships it later.

**Q: Exactly-once or at-least-once?**
A: At-least-once. The relay can publish, crash before marking published, and republish. Idempotent consumers (keyed on `eventId`) absorb the duplicate.

**Q: How many consumers can read one topic in parallel within a group?**
A: Up to the partition count. Extra consumers in the same group idle.

**Q: What triggers a rebalance and why care?**
A: A consumer joining or leaving the group. It briefly pauses consumption and can replay a few records — handle duplicates.

**Q: Poison message — what do you do?**
A: Retry with backoff; after N attempts move it to a dead-letter topic and alert, so one bad record can't block the partition.
