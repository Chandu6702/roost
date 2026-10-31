package com.chandu.roost.messaging;

import com.chandu.roost.service.WebhookService;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes incident events off Kafka and fans out notifications. This runs
 * asynchronously from the HTTP request (which returned 201 long ago) and from
 * the outbox relay that produced the event.
 *
 * <p>{@link WebhookService} is optional ({@code roost.webhook.enabled}); we take
 * it via {@link ObjectProvider} so the consumer still works with webhooks off.
 */
@Component
public class NotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final ObjectProvider<WebhookService> webhookService;

    public NotificationConsumer(
            ObjectMapper objectMapper,
            ObjectProvider<WebhookService> webhookService) {
        this.objectMapper = objectMapper;
        this.webhookService = webhookService;
    }

    @KafkaListener(
            topics = "${roost.events.topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onIncidentEvent(String message) {
        OutboxMessage event;
        try {
            event = objectMapper.readValue(message, OutboxMessage.class);
        } catch (Exception e) {
            // Malformed payload: log and move on rather than stalling the
            // partition. A real deployment routes these to a dead-letter topic.
            log.error("Discarding unparseable incident event: {}", e.getMessage());
            return;
        }

        if (!"INCIDENT_OPENED".equals(event.eventType())) {
            return;
        }

        Long workspaceId = asLong(event.payload().get("workspaceId"));
        if (workspaceId == null) {
            log.warn("Incident event {} has no workspaceId; skipping fan-out",
                    event.aggregateId());
            return;
        }

        log.info("Incident {} opened for monitor {} — fanning out to workspace {}",
                event.aggregateId(), event.payload().get("monitorId"), workspaceId);

        // Deliver the exact message body we received so subscribers can verify
        // the HMAC signature against identical bytes.
        webhookService.ifAvailable(service -> service.deliver(workspaceId, message));
    }

    private static Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
