package com.chandu.roost.entity;

import jakarta.persistence.*;

import java.time.Instant;
@Entity
@Table(name = "incidents")
public class Incident extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optimistic lock. Hibernate appends {@code AND version = ?} to every update
     * and bumps it; a concurrent update that read the same version updates zero
     * rows and throws {@code ObjectOptimisticLockingFailureException} (→ HTTP 409).
     */
    @Version
    private Long version;

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

    public Long getVersion() {
        return version;
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

    public void setAcknowledgedAt(Instant now) {
        this.acknowledgedAt = now;
    }

    public void acknowledge() {
        if (status != IncidentStatus.OPEN) {
            throw new IllegalStateException(
                    "Only OPEN incidents can be acknowledged"
            );
        }

        this.status = IncidentStatus.ACK;
        this.acknowledgedAt = Instant.now();
    }

    public void resolve() {
        if (status != IncidentStatus.ACK) {
            throw new IllegalStateException(
                    "Only ACK incidents can be resolved"
            );
        }

        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    /**
     * Automatic recovery: when the monitor comes back UP we resolve the active
     * incident regardless of whether anyone acknowledged it. Distinct from the
     * manual {@link #resolve()} (which enforces OPEN → ACK → RESOLVED) because a
     * service recovering on its own shouldn't need a human ack first.
     */
    public void resolveOnRecovery() {
        if (status == IncidentStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Incident is already resolved"
            );
        }

        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }
}