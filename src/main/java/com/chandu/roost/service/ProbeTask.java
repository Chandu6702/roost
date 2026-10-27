
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

    public ProbeTask(
            ProbeScheduleRepository probeScheduleRepository,
            ProbeResultRepository probeResultRepository,
            ProbeRunner probeRunner
    ) {
        this.probeScheduleRepository = probeScheduleRepository;
        this.probeResultRepository = probeResultRepository;
        this.probeRunner = probeRunner;

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
        try {
            probeRunner.run(monitor.getUrl());

            int latencyMs = (int) (
                    (System.nanoTime() - startNanos) / 1_000_000
            );

            probeResultRepository.save(
                    new ProbeResult(
                            monitor,
                            probedAt,
                            "UP",
                            latencyMs,
                            null
                    )
            );

            log.info(
                    "Probe completed for monitor {} in {} ms",
                    monitor.getId(),
                    latencyMs
            );
        } catch (Exception exception) {
            int latencyMs = (int) (
                    (System.nanoTime() - startNanos) / 1_000_000
            );

            probeResultRepository.save(
                    new ProbeResult(
                            monitor,
                            probedAt,
                            "DOWN",
                            latencyMs,
                            exception.getMessage()
                    )
            );

            log.warn(
                    "Probe failed for monitor {}: {}",
                    monitor.getId(),
                    exception.getMessage()
            );
        }
    }
}
