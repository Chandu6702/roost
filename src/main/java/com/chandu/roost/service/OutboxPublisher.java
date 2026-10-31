package com.chandu.roost.service;

import com.chandu.roost.entity.OutboxEvent;
import com.chandu.roost.messaging.OutboxMessage;
import com.chandu.roost.repository.OutboxEventRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The relay half of the transactional outbox. A business transaction writes an
 * {@link OutboxEvent} in the same DB commit as its state change; this poller
 * later ships those rows to Kafka and marks them published.
 *
 * <p>Delivery is <b>at-least-once</b>: we only set {@code publishedAt} after the
 * broker acknowledges the send, so a crash between send and commit re-publishes
 * on the next poll. Consumers must therefore be idempotent (we key the envelope
 * with {@code eventId} for that purpose).
 */
@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${roost.events.topic}") String topic) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            // Key by aggregate so all events for one incident land on one
            // partition and stay ordered. Publishing stops on the first failure
            // so we never ship a later event before an earlier one.
            if (!publish(event)) {
                break;
            }
            event.markPublished();
        }
    }

    private boolean publish(OutboxEvent event) {
        String key = event.getAggregateType() + ":" + event.getAggregateId();

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
            // Non-retryable: a row we can never serialize would block the queue
            // forever. Drop it (mark published) and alert instead of looping.
            // (Jackson 3 throws this unchecked; we still handle it explicitly.)
            log.error("Dropping unserializable outbox event id={}: {}",
                    event.getId(), e.getMessage());
            return true;
        }

        try {
            kafkaTemplate.send(topic, key, json).get(5, TimeUnit.SECONDS);
            log.info("Published outbox event id={} type={} to {}",
                    event.getId(), event.getEventType(), topic);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted publishing outbox event id={}", event.getId());
            return false;
        } catch (Exception e) {
            // Broker down / timeout: leave the row unpublished and retry next poll.
            log.warn("Kafka publish failed for outbox event id={}, will retry: {}",
                    event.getId(), e.getMessage());
            return false;
        }
    }
}
