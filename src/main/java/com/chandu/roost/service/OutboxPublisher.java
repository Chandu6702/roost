
package com.chandu.roost.service;

import com.chandu.roost.entity.OutboxEvent;
import com.chandu.roost.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(readOnly = true)
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository
                        .findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            log.info(
                    "Outbox event ready for publishing: id={}, type={}, aggregateType={}, aggregateId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId()
            );
        }
    }
}
