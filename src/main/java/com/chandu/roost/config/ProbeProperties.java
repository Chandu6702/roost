package com.chandu.roost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roost.probe")
public record ProbeProperties(
    int defaultIntervalSeconds,
    int defaultTimeoutMillis
) {
}
