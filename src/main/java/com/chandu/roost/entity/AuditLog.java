package com.chandu.roost.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(Incident incident, String action, Instant createdAt) {
        this.incident = incident;
        this.action = action;
        this.createdAt = createdAt;
    }

    public AuditLog(Monitor monitor, String action, Instant createdAt) {
        this.monitor = monitor;
        this.action = action;
        this.createdAt = createdAt;
    }
    public Long getId() {
        return id;
    }

    public Monitor getMonitor() {
        return monitor;
    }
    public Incident getIncident() {
        return incident;
    }

    public String getAction() {
        return action;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}