package com.chandu.roost.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record MonitorResponse(
        Long id,
        Long workspaceId,
        String name,
        String url,

        @JsonProperty("interval_seconds")
        Integer intervalSeconds,

        @JsonProperty("timeout_millis")
        Integer timeoutMillis,

        MonitorType type,
        Instant createdAt
) {
}