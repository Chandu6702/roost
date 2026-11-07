package com.chandu.roost.outbox;
import com.chandu.roost.incident.Incident;
import com.chandu.roost.notification.WebhookService;
import com.chandu.roost.notification.NotificationConsumer;

import com.chandu.roost.notification.OutboxMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The broker-free sink for the transactional outbox, and the default. Instead
 * of going through Kafka it fans out the webhook in-process — the same work
 * {@link com.chandu.roost.notification.NotificationConsumer} does on the Kafka
 * path, minus the hop. This lets the app run on just Postgres + Redis with no
 * message broker while keeping the outbox's at-least-once guarantee: the
 * {@link OutboxPublisher} still only marks a row published once this returns
 * {@code true}.
 *
 * <p>Active unless {@code roost.kafka.enabled=true}, in which case
 * {@link KafkaOutboxDispatcher} takes over.
 *
 * <p>{@link WebhookService} is optional ({@code roost.webhook.enabled}); taken
 * via {@link ObjectProvider} so this still works with webhooks off.
 */
@Component
@ConditionalOnProperty(
        name = "roost.kafka.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DirectOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(DirectOutboxDispatcher.class);

    private final ObjectProvider<WebhookService> webhookService;
    private final ObjectMapper objectMapper;

    public DirectOutboxDispatcher(
            ObjectProvider<WebhookService> webhookService,
            ObjectMapper objectMapper) {

        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean dispatch(OutboxEvent event) {
        // Only incident-opened events fan out (mirrors the Kafka consumer).
        if (!"INCIDENT_OPENED".equals(event.getEventType())) {
            return true;
        }

        // Serialise the same envelope the Kafka path would, so a subscriber's
        // HMAC is computed over identical bytes whichever path delivered it.
        String json;
        try {
            json = objectMapper.writeValueAsString(new OutboxMessage(
                    event.getId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getPayload(),
                    event.getCreatedAt()
            ));
        } catch (JacksonException e) {
            // Unserializable row: drop it rather than retry forever (same policy
            // as the Kafka path).
            log.error("Dropping unserializable outbox event id={}: {}",
                    event.getId(), e.getMessage());
            return true;
        }

        Long workspaceId = asLong(event.getPayload().get("workspaceId"));
        if (workspaceId == null) {
            log.warn("Incident event id={} has no workspaceId; skipping fan-out",
                    event.getId());
            return true;
        }

        log.info("Incident {} opened — fanning out webhooks in-process to workspace {}",
                event.getAggregateId(), workspaceId);

        // WebhookService (and its retry/backoff) handles delivery; it already
        // gives up after maxAttempts, so there is nothing to retry at this level.
        webhookService.ifAvailable(service -> service.deliver(workspaceId, json));
        return true;
    }

    private static Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
