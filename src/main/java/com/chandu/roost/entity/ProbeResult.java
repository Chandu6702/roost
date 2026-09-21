package com.chandu.roost.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "probe_results")
public class ProbeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(name = "probed_at", nullable = false)
    private Instant probedAt;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    protected ProbeResult() {
    }

    public ProbeResult(
            Monitor monitor,
            Instant probedAt,
            String status,
            Integer latencyMs,
            String errorMessage) {

        this.monitor = monitor;
        this.probedAt = probedAt;
        this.status = status;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public Instant getProbedAt() {
        return probedAt;
    }

    public String getStatus() {
        return status;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
}