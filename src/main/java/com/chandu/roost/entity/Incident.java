package com.chandu.roost.entity;

import jakarta.persistence.*;

import java.time.Instant;
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(length = 2048)
    private String summary;

    protected Incident() {
    }

    public Incident(
            Monitor monitor,
            IncidentStatus status,
            Instant openedAt,
            String summary) {

        this.monitor = monitor;
        this.status = status;
        this.openedAt = openedAt;
        this.summary = summary;
    }

    public Long getId() {
        return id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setStatus(IncidentStatus incidentStatus) {
        this.status = incidentStatus;
    }

    public void setResolvedAt(Instant now) {
        this.resolvedAt = now;
    }
}