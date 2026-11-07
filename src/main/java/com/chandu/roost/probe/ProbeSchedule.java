package com.chandu.roost.probe;
import com.chandu.roost.monitor.Monitor;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "probe_schedule")
public class ProbeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false, unique = true)
    private Monitor monitor;

    @Column(name = "next_probe_at", nullable = false)
    private Instant nextProbeAt;

    protected ProbeSchedule() {
    }

    public ProbeSchedule(Monitor monitor, Instant nextProbeAt) {
        this.monitor = monitor;
        this.nextProbeAt = nextProbeAt;
    }

    public Long getId() {
        return id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public Instant getNextProbeAt() {
        return nextProbeAt;
    }

    public void setNextProbeAt(Instant nextProbeAt) {
        this.nextProbeAt = nextProbeAt;
    }
}
