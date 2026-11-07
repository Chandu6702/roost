package com.chandu.roost.probe;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ProbeScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ProbeScheduler.class);

    private final ProbeScheduleRepository probeScheduleRepository;
    private final ObjectProvider<ProbeTask> probeTaskProvider;

    public ProbeScheduler(
            ProbeScheduleRepository probeScheduleRepository,
            ObjectProvider<ProbeTask> probeTaskProvider
    ) {
        this.probeScheduleRepository = probeScheduleRepository;
        this.probeTaskProvider = probeTaskProvider;
    }

    @PostConstruct
    public void initialize() {
        log.info("Probe scheduler ready");
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void discoverDueSchedules() {
        Instant cutoff = Instant.now();

        List<ProbeSchedule> dueSchedules =
                probeScheduleRepository
                        .findByNextProbeAtLessThanEqual(cutoff);

        for (ProbeSchedule schedule : dueSchedules) {
            long intervalSeconds =
                    schedule.getMonitor().getIntervalSeconds();

            Instant nextRunAt =
                    cutoff.plusSeconds(intervalSeconds);

            int claimed = probeScheduleRepository.claimDueSchedule(
                    schedule.getId(),
                    cutoff,
                    nextRunAt
            );

            if (claimed == 1) {
                log.info(
                        "Dispatching probe for schedule {}",
                        schedule.getId()
                );

                probeTaskProvider.getObject()
                        .execute(schedule.getId());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Probe scheduler shutting down");
    }
}
