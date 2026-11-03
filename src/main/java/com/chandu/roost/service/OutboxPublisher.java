package com.chandu.roost.service;

import com.chandu.roost.entity.OutboxEvent;
import com.chandu.roost.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The relay half of the transactional outbox. A business transaction writes an
 * {@link OutboxEvent} in the same DB commit as its state change; this poller
 * later ships those rows onward through the active {@link OutboxDispatcher} and
 * marks them published.
 *
 * <p>Delivery is <b>at-least-once</b>: a row is only marked {@code publishedAt}
 * after the dispatcher reports success, so a crash mid-dispatch re-ships on the
 * next poll. Downstream consumers must therefore be idempotent (the envelope
 * carries {@code eventId} for that purpose).
 *
 * <p><em>How</em> an event leaves the app is the dispatcher's concern — Kafka
 * ({@link KafkaOutboxDispatcher}) or in-process webhook fan-out
 * ({@link DirectOutboxDispatcher}) — selected by {@code roost.kafka.enabled}.
 * This class is unchanged by that choice: it owns only the polling, ordering
 * and publish bookkeeping.
 */
@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxDispatcher dispatcher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxDispatcher dispatcher) {

        this.outboxEventRepository = outboxEventRepository;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            // Stop on the first failure so a later event never overtakes an
            // earlier one; unpublished rows are retried on the next poll.
            if (!dispatcher.dispatch(event)) {
                break;
            }
            event.markPublished();
        }
    }
}
