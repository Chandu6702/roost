
package com.chandu.roost.service;

import com.chandu.roost.aop.LogExecutionTime;
import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.OutboxEvent;
import com.chandu.roost.error.IncidentNotFoundException;
import com.chandu.roost.error.MonitorNotFoundException;
import com.chandu.roost.repository.IncidentRepository;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final MonitorRepository monitorRepository;
    private final OutboxEventRepository outboxEventRepository;

    public IncidentService(
            IncidentRepository incidentRepository,
            AuditService auditService,
            MonitorRepository monitorRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
        this.monitorRepository = monitorRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @LogExecutionTime
    @Transactional
    public Incident open(Long monitorId, String summary) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(monitorId));

        Incident incident = incidentRepository.save(
                new Incident(
                        monitor,
                        IncidentStatus.OPEN,
                        Instant.now(),
                        summary
                )
        );

        auditService.logStateChange(incident, "INCIDENT_OPENED");
        recordEvent(incident, monitor, "INCIDENT_OPENED");

        return incident;
    }

    /**
     * Opens an incident only if the monitor has no active one (OPEN or ACK).
     * Called from the probe pipeline on a DOWN result — dedupes so a monitor
     * that stays down for an hour produces one incident, not one per probe.
     */
    @Transactional
    public void openIfDown(Long monitorId, String summary) {
        boolean alreadyActive = incidentRepository.existsByMonitorIdAndStatusIn(
                monitorId,
                List.of(IncidentStatus.OPEN, IncidentStatus.ACK));

        if (!alreadyActive) {
            open(monitorId, summary);
        }
    }

    /**
     * Auto-resolves any active incident for a monitor when it comes back UP.
     * Each resolution is audited and emits an INCIDENT_RESOLVED outbox event.
     */
    @Transactional
    public void resolveOnRecovery(Long monitorId) {
        List<Incident> active = incidentRepository.findByMonitorIdAndStatusIn(
                monitorId,
                List.of(IncidentStatus.OPEN, IncidentStatus.ACK));

        for (Incident incident : active) {
            incident.resolveOnRecovery();
            auditService.logStateChange(incident, "INCIDENT_RESOLVED");
            recordEvent(incident, incident.getMonitor(), "INCIDENT_RESOLVED");
        }
    }

    private void recordEvent(Incident incident, Monitor monitor, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.getId());
        payload.put("monitorId", monitor.getId());
        // Carried so the async notification consumer can fan out to the right tenant.
        payload.put("workspaceId", monitor.getWorkspace().getId());
        payload.put("status", incident.getStatus().name());
        payload.put("summary", incident.getSummary());
        payload.put("openedAt", incident.getOpenedAt().toString());

        outboxEventRepository.save(new OutboxEvent(
                "Incident",
                incident.getId(),
                eventType,
                payload
        ));
    }

    @Transactional
    public void resolve(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        incident.resolve();
        incidentRepository.save(incident);

        auditService.logStateChange(incident, "INCIDENT_RESOLVED");
    }

    @Transactional
    public void acknowledge(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        incident.acknowledge();
        incidentRepository.save(incident);

        auditService.logStateChange(incident, "INCIDENT_ACKNOWLEDGED");
    }
}
