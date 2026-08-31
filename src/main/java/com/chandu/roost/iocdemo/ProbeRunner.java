package com.chandu.roost.iocdemo;

public class ProbeRunner {

    private final HttpProbeClient httpProbeClient;

    public ProbeRunner(HttpProbeClient httpProbeClient) {
        this.httpProbeClient = httpProbeClient;
    }

    public String run(String url) {
        return httpProbeClient.probe(url);
    }
}