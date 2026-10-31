
package com.chandu.roost.service;

import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.ProbeResult;
import com.chandu.roost.entity.ProbeSchedule;
import com.chandu.roost.probe.ProbeRunner;
import com.chandu.roost.repository.ProbeResultRepository;
import com.chandu.roost.repository.ProbeScheduleRepository;
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

    public ProbeTask(
            ProbeScheduleRepository probeScheduleRepository,
            ProbeResultRepository probeResultRepository,
            ProbeRunner probeRunner,
            IncidentService incidentService
    ) {
        this.probeScheduleRepository = probeScheduleRepository;
        this.probeResultRepository = probeResultRepository;
        this.probeRunner = probeRunner;
        this.incidentService = incidentService;

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

        int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
        String status = healthy ? "UP" : "DOWN";

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
