
package com.chandu.roost.probe;
import com.chandu.roost.incident.IncidentService;

import com.chandu.roost.monitor.Monitor;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProbeTask {

    private static final Logger log =
            LoggerFactory.getLogger(ProbeTask.class);

    private final ProbeScheduleRepository probeScheduleRepository;
    private final ProbeResultRepository probeResultRepository;
    private final ProbeRunner probeRunner;
    private final IncidentService incidentService;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public ProbeTask(
            ProbeScheduleRepository probeScheduleRepository,
            ProbeResultRepository probeResultRepository,
            ProbeRunner probeRunner,
            IncidentService incidentService,
            io.micrometer.core.instrument.MeterRegistry meterRegistry
    ) {
        this.probeScheduleRepository = probeScheduleRepository;
        this.probeResultRepository = probeResultRepository;
        this.probeRunner = probeRunner;
        this.incidentService = incidentService;
        this.meterRegistry = meterRegistry;

        log.debug("ProbeTask instance created");
    }

    @Async
    @Transactional
    public void execute(Long scheduleId) {
        ProbeSchedule schedule = probeScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Probe schedule not found: " + scheduleId
                ));

        Monitor monitor = schedule.getMonitor();
        Instant probedAt = Instant.now();
        long startNanos = System.nanoTime();

        boolean healthy;
        String errorMessage;
        try {
            var outcome = probeRunner.run(
                    monitor.getUrl(),
                    monitor.getTimeoutMillis()
            );
            healthy = outcome.healthy();
            errorMessage = outcome.errorMessage();
        } catch (Exception exception) {
            healthy = false;
            errorMessage = exception.getMessage();
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        int latencyMs = (int) (elapsedNanos / 1_000_000);
        String status = healthy ? "UP" : "DOWN";

        // Metrics: low-cardinality (tag by outcome only, never by monitor/url).
        meterRegistry.counter("roost.probes.executed",
                "outcome", healthy ? "up" : "down").increment();
        meterRegistry.timer("roost.probes.duration")
                .record(elapsedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        probeResultRepository.save(
                new ProbeResult(monitor, probedAt, status, latencyMs, errorMessage)
        );

        // Drive the incident state machine off the probe result. Same
        // transaction as the ProbeResult insert, so the result, the incident,
        // its audit row and the outbox event all commit (or roll back) together.
        if (healthy) {
            log.info("Probe UP for monitor {} in {} ms", monitor.getId(), latencyMs);
            incidentService.resolveOnRecovery(monitor.getId());
        } else {
            log.warn("Probe DOWN for monitor {}: {}", monitor.getId(), errorMessage);
            incidentService.openIfDown(
                    monitor.getId(),
                    "Probe failed: " + (errorMessage != null ? errorMessage : "unknown"));
        }
    }
}
