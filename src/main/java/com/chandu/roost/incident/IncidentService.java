
package com.chandu.roost.incident;
import com.chandu.roost.audit.AuditService;
import com.chandu.roost.monitor.MonitorService;

import com.chandu.roost.shared.observability.LogExecutionTime;
import com.chandu.roost.monitor.Monitor;
import com.chandu.roost.outbox.OutboxEvent;
import com.chandu.roost.monitor.MonitorNotFoundException;
import com.chandu.roost.monitor.MonitorRepository;
import com.chandu.roost.outbox.OutboxEventRepository;
import org.springframework.data.domain.Pageable;
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
    private final MonitorService monitorService;

    public IncidentService(
            IncidentRepository incidentRepository,
            AuditService auditService,
            MonitorRepository monitorRepository,
            OutboxEventRepository outboxEventRepository,
            MonitorService monitorService
    ) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
        this.monitorRepository = monitorRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.monitorService = monitorService;
    }

    /**
     * Incidents across the caller's workspace, newest first. Tenant isolation
     * is inherited from {@link MonitorService#getMonitors(String)} — we only
     * ever query incidents for monitor IDs that scoped lookup returned, so a
     * caller can never see another workspace's incidents.
     */
    @Transactional(readOnly = true)
    public List<IncidentResponse> listForUser(String email, Pageable pageable) {
        List<Long> monitorIds = monitorService.getMonitors(email).stream()
                .map(Monitor::getId)
                .toList();

        if (monitorIds.isEmpty()) {
            return List.of();
        }

        return incidentRepository
                .findByMonitorIdInOrderByOpenedAtDesc(monitorIds, pageable)
                .stream()
                .map(i -> new IncidentResponse(
                        i.getId(),
                        i.getMonitor().getId(),
                        i.getMonitor().getName(),
                        i.getStatus(),
                        i.getOpenedAt(),
                        i.getAcknowledgedAt(),
                        i.getResolvedAt(),
                        i.getSummary()
                ))
                .toList();
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
