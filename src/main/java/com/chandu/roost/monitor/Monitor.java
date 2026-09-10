package com.chandu.roost.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Monitor {
    private Long id;
    private String name;
    private String url;
    private String status;
    @JsonProperty("check_interval_seconds")
    private Integer checkIntervalSeconds;
    private MonitorType type;

    public Monitor(Long id, String name, String url, String status, Integer checkIntervalSeconds, MonitorType type) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.status = status;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.type = type;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public MonitorType getType() {
        return type;
    }

}
