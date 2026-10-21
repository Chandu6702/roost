package com.chandu.roost.service;

import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.repository.IncidentRepository;
import com.chandu.roost.repository.MonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chandu.roost.aop.LogExecutionTime;

import java.time.Instant;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final MonitorRepository monitorRepository;


    public IncidentService(
            IncidentRepository incidentRepository,
            AuditService auditService,
            MonitorRepository monitorRepository
            ) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
        this.monitorRepository = monitorRepository;
    }

    @LogExecutionTime
    @Transactional
    public Incident open(Long monitorId, String summary) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new RuntimeException(
                        "Monitor not found"
                ));

        Incident incident = new Incident(
                monitor,
                IncidentStatus.OPEN,
                Instant.now(),
                summary
        );

        return incidentRepository.save(incident);
    }

    @Transactional
    public void resolve(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        incident.resolve();

        incidentRepository.save(incident);

        auditService.logStateChange(
                incident,
                "INCIDENT_RESOLVED"
        );

        throw new RuntimeException("Forced failure");
    }

    @Transactional
    public void acknowledge(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        incident.acknowledge();

        incidentRepository.save(incident);

        auditService.logStateChange(
                incident,
                "INCIDENT_ACKNOWLEDGED"
        );
    }
}