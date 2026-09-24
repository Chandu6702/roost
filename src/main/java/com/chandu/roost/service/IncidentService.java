package com.chandu.roost.service;

import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public IncidentService(
            IncidentRepository incidentRepository,
            AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void resolve(Long incidentId) {

        // 1. Find the incident
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        // 2. Change incident state
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());

        // 3. Save the incident
        incidentRepository.save(incident);

        // 4. Write audit in an independent transaction
        auditService.logStateChange(
                incident,
                "INCIDENT_RESOLVED"
        );

        // 5. Force outer transaction to fail
        throw new RuntimeException("Forced failure");
    }
}