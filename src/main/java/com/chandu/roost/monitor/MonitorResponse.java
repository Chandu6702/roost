package com.chandu.roost.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MonitorResponse(
    Long id,
    String name,
    String url,
    String status,
    @JsonProperty("check_interval_seconds")
    Integer checkIntervalSeconds,
    MonitorType type
) {
}
