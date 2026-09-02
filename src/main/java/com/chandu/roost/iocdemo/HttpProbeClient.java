package com.chandu.roost.iocdemo;

import org.springframework.stereotype.Component;

@Component
public class HttpProbeClient {

    public String probe(String url) {
        return "Stub response from " + url;
    }
}