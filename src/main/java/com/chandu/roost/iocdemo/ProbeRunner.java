package com.chandu.roost.iocdemo;

import org.springframework.stereotype.Component;

@Component
public class ProbeRunner {

    private final HttpProbeClient httpProbeClient;

    public ProbeRunner(HttpProbeClient httpProbeClient) {
        this.httpProbeClient = httpProbeClient;
    }

    public String run(String url) {
        return httpProbeClient.probe(url);
    }
}