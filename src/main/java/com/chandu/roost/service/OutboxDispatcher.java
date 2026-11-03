package com.chandu.roost.service;

import com.chandu.roost.entity.OutboxEvent;

/**
 * Strategy for shipping one outbox row onward. Exactly one implementation is
 * active, selected by {@code roost.kafka.enabled}:
 * {@link KafkaOutboxDispatcher} publishes to Kafka (the full producer/consumer
 * pipeline), while {@link DirectOutboxDispatcher} fans out the webhook
 * in-process with no broker — the default, so the app runs on just Postgres +
 * Redis.
 *
 * <p>{@link OutboxPublisher} owns the polling, ordering and at-least-once
 * bookkeeping; a dispatcher only decides <em>how</em> a single event leaves the
 * application.
 */
public interface OutboxDispatcher {

    /**
     * Handle one event.
     *
     * @return {@code true} if the event was handled and may be marked published;
     *         {@code false} to leave it unpublished and retry on the next poll
     *         (the publisher then stops, so a later event never overtakes it).
     */
    boolean dispatch(OutboxEvent event);
}
