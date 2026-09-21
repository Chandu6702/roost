package com.chandu.roost.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MonitorType type;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds;

    @Column(name = "timeout_millis", nullable = false)
    private Integer timeoutMillis;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "monitor")
    private List<ProbeResult> probeResults = new ArrayList<>();
    protected Monitor() {
    }

    public Monitor(
            Workspace workspace,
            String name,
            String url,
            MonitorType type,
            Integer intervalSeconds,
            Integer timeoutMillis,
            Instant createdAt) {

        this.workspace = workspace;
        this.name = name;
        this.url = url;
        this.type = type;
        this.intervalSeconds = intervalSeconds;
        this.timeoutMillis = timeoutMillis;
        this.createdAt = createdAt;
    }

    public void addProbeResult(ProbeResult probeResult){
        probeResults.add(probeResult);
        probeResult.setMonitor(this);
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public MonitorType getType() {
        return type;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public Integer getTimeoutMillis() {
        return timeoutMillis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setType(MonitorType type) {
        this.type = type;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public void setTimeoutMillis(Integer timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}