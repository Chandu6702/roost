package com.chandu.roost.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ProbeScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ProbeScheduler.class);

    private final ObjectProvider<ProbeTask> probeTaskProvider;

    public ProbeScheduler(ObjectProvider<ProbeTask> probeTaskProvider) {
        this.probeTaskProvider = probeTaskProvider;
    }

    public void runProbeTask() {
        ProbeTask task = probeTaskProvider.getObject();
        log.info("Running probe task");
    }

    @PostConstruct
    public void initialize() {
        log.info("Probe scheduler ready");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Probe scheduler shutting down");
    }
}