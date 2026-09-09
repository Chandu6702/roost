package com.chandu.roost.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MonitorRequest(
    String name,
    String url,
    String status,
    @JsonProperty("check_interval_seconds")
    Integer checkIntervalSeconds
) {
}