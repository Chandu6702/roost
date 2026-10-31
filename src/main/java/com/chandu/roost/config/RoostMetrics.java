package com.chandu.roost.config;

import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.repository.IncidentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Registers Roost's domain gauges with Micrometer. Counters and timers are
 * created where the event happens (see {@code ProbeTask}); gauges are
 * point-in-time and are bound here to a supplier the registry polls on scrape.
 *
 * <p>Deliberately low-cardinality: we tag by outcome (up/down), never by
 * monitor id or URL, which would explode the number of time series.
 */
@Component
public class RoostMetrics {

    private final MeterRegistry registry;
    private final IncidentRepository incidentRepository;

    public RoostMetrics(MeterRegistry registry,
                        IncidentRepository incidentRepository) {
        this.registry = registry;
        this.incidentRepository = incidentRepository;
    }

    @PostConstruct
    void bindGauges() {
        Gauge.builder("roost.incidents.open",
                        () -> incidentRepository.countByStatus(IncidentStatus.OPEN))
                .description("Currently open (unacknowledged) incidents")
                .register(registry);
    }
}
