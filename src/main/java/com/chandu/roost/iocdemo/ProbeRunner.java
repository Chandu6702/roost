package com.chandu.roost.iocdemo;

import com.chandu.roost.config.ProbeProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProbeRunner {

    private static final Logger log = LoggerFactory.getLogger(ProbeRunner.class);

    private final HttpProbeClient httpProbeClient;
    private final ProbeProperties probeProperties;

    public ProbeRunner(HttpProbeClient httpProbeClient, ProbeProperties probeProperties) {
        this.httpProbeClient = httpProbeClient;
        this.probeProperties = probeProperties;
    }

    @PostConstruct
    public void init() {
        log.info("ProbeRunner initialized with properties: {}", probeProperties);
    }

    public String run(String url) {
        return httpProbeClient.probe(url);
    }
}