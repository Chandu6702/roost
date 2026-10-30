
package com.chandu.roost.service;

import com.chandu.roost.aop.LogExecutionTime;
import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.OutboxEvent;
import com.chandu.roost.repository.IncidentRepository;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
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
                .orElseThrow(() -> new RuntimeException("Monitor not found"));

        Incident incident = incidentRepository.save(
                new Incident(
                        monitor,
                        IncidentStatus.OPEN,
                        Instant.now(),
                        summary
                )
        );

        auditService.logStateChange(incident, "INCIDENT_OPENED");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.getId());
        payload.put("monitorId", monitor.getId());
        payload.put("status", incident.getStatus().name());
        payload.put("summary", incident.getSummary());
        payload.put("openedAt", incident.getOpenedAt().toString());

        OutboxEvent event = new OutboxEvent(
                "Incident",
                incident.getId(),
                "INCIDENT_OPENED",
                payload
        );

        outboxEventRepository.save(event);

        return incident;
    }

    @Transactional
    public void resolve(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        incident.resolve();
        incidentRepository.save(incident);

        auditService.logStateChange(incident, "INCIDENT_RESOLVED");
    }

    @Transactional
    public void acknowledge(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        incident.acknowledge();
        incidentRepository.save(incident);

        auditService.logStateChange(incident, "INCIDENT_ACKNOWLEDGED");
    }
}
