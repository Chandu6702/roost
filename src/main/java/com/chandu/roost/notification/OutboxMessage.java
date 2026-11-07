package com.chandu.roost.notification;

import java.time.Instant;
import java.util.Map;

/**
 * The JSON envelope that travels over Kafka. It mirrors one outbox row.
 * {@code eventId} lets consumers deduplicate (outbox delivery is at-least-once,
 * so the same event can legitimately arrive twice).
 */
public record OutboxMessage(
        Long eventId,
        String aggregateType,
        Long aggregateId,
        String eventType,
        Map<String, Object> payload,
        Instant occurredAt
) {
}
