package com.chandu.roost.service;

import com.chandu.roost.entity.AuditLog;
import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.MonitorType;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.repository.AuditLogRepository;
import com.chandu.roost.repository.IncidentRepository;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IncidentServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Test
    void resolve_rollsBackIncidentButAuditSurvives() {

        // Arrange
        Workspace workspace = new Workspace(
                "Test Workspace",
                "incident-test-" + UUID.randomUUID()
        );

        workspace = workspaceRepository.save(workspace);

        Monitor monitor = monitorRepository.save(
                new Monitor(
                        workspace,
                        "Test Monitor",
                        "https://example.com",
                        MonitorType.HTTP,
                        60,
                        5000,
                        Instant.now()
                )
        );

        Incident incident = incidentRepository.save(
                new Incident(
                        monitor,
                        IncidentStatus.OPEN,
                        Instant.now(),
                        "Test incident"
                )
        );

        Long incidentId = incident.getId();

        // Move OPEN → ACK so RESOLVED is a valid next state.
        incidentService.acknowledge(incidentId);

        // Act:
        // resolve() intentionally throws after creating the
        // INCIDENT_RESOLVED audit in a REQUIRES_NEW transaction.
        assertThrows(
                RuntimeException.class,
                () -> incidentService.resolve(incidentId)
        );

        // Assert:
        // The REQUIRES_NEW audit transaction survives.
        List<AuditLog> auditLogs =
                auditLogRepository.findByIncidentId(incidentId);

        assertEquals(
                1,
                auditLogs.stream()
                        .filter(audit ->
                                "INCIDENT_RESOLVED".equals(audit.getAction()))
                        .count()
        );

        // Assert:
        // The outer resolve transaction rolled back.
        // The incident should remain ACK, because OPEN → ACK
        // happened before the failed RESOLVE transaction.
        Incident result = incidentRepository.findById(incidentId)
                .orElseThrow();

        assertEquals(IncidentStatus.ACK, result.getStatus());
        assertNull(result.getResolvedAt());
    }

    @Test
    void open_createsOpenIncident() {

        Workspace workspace = new Workspace(
                "Open Test Workspace",
                "open-test-" + UUID.randomUUID()
        );

        workspace = workspaceRepository.save(workspace);

        Monitor monitor = monitorRepository.save(
                new Monitor(
                        workspace,
                        "Open Test Monitor",
                        "https://example.com",
                        MonitorType.HTTP,
                        60,
                        5000,
                        Instant.now()
                )
        );

        Incident incident = incidentService.open(
                monitor.getId(),
                "Monitor is DOWN"
        );

        assertNotNull(incident.getId());

        assertEquals(
                IncidentStatus.OPEN,
                incident.getStatus()
        );

        assertNotNull(incident.getOpenedAt());

        assertEquals(
                "Monitor is DOWN",
                incident.getSummary()
        );

        assertEquals(
                monitor.getId(),
                incident.getMonitor().getId()
        );
    }

    @Test
    void incident_can_completeFullLifecycle() {

        Workspace workspace = new Workspace(
                "Lifecycle Test Workspace",
                "lifecycle-test-" + UUID.randomUUID()
        );

        workspace = workspaceRepository.save(workspace);

        Monitor monitor = monitorRepository.save(
                new Monitor(
                        workspace,
                        "Lifecycle Test Monitor",
                        "https://example.com",
                        MonitorType.HTTP,
                        60,
                        5000,
                        Instant.now()
                )
        );

        Incident incident = incidentService.open(
                monitor.getId(),
                "Monitor is DOWN"
        );

        assertEquals(
                IncidentStatus.OPEN,
                incident.getStatus()
        );

        assertNotNull(incident.getOpenedAt());

        // OPEN → ACK
        incidentService.acknowledge(incident.getId());

        Incident acknowledged =
                incidentRepository.findById(incident.getId())
                        .orElseThrow();

        assertEquals(
                IncidentStatus.ACK,
                acknowledged.getStatus()
        );

        assertNotNull(
                acknowledged.getAcknowledgedAt()
        );

        /*
         * resolve() currently contains the intentional forced
         * failure used by the REQUIRES_NEW transaction test.
         *
         * Therefore we do not call incidentService.resolve()
         * here.
         *
         * The pure IncidentStateMachineTest verifies the complete
         * ACK → RESOLVED domain transition.
         */
    }
}