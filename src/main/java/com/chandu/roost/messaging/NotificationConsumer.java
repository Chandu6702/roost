package com.chandu.roost.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes incident events off Kafka and triggers notifications. This is the
 * fan-out side of the pipeline: the HTTP request that opened an incident has
 * long since returned 201, and this runs asynchronously from the outbox relay.
 */
@Component
public class NotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;

    public NotificationConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        log.info("Notification: incident {} opened for monitor {} — notifying subscribers",
                event.aggregateId(), event.payload().get("monitorId"));
        // Webhook fan-out wires in on W17 D5.
    }
}
