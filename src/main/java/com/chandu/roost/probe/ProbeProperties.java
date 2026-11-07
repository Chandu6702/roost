package com.chandu.roost.probe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roost.probe")
public record ProbeProperties(
    int defaultIntervalSeconds,
    int defaultTimeoutMillis
) {
}
