package com.chandu.roost.outbox;
import com.chandu.roost.notification.NotificationConsumer;

import com.chandu.roost.notification.OutboxMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * The Kafka sink for the transactional outbox: serialises each row to an
 * {@link OutboxMessage} envelope and publishes it to the incidents topic, keyed
 * by aggregate so all events for one incident land on one partition and stay
 * ordered. {@link com.chandu.roost.notification.NotificationConsumer} reads them
 * back and fans out the webhooks.
 *
 * <p>Active only when {@code roost.kafka.enabled=true}; otherwise
 * {@link DirectOutboxDispatcher} delivers in-process without a broker.
 */
@Component
@ConditionalOnProperty(name = "roost.kafka.enabled", havingValue = "true")
public class KafkaOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaOutboxDispatcher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaOutboxDispatcher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${roost.events.topic}") String topic) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public boolean dispatch(OutboxEvent event) {
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
