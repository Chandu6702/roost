package com.chandu.roost.service;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IncidentAcknowledgeTest {

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
    void acknowledge_movesIncidentFromOpenToAckAndCreatesAudit() {

        // Arrange
        Workspace workspace = workspaceRepository.save(
                new Workspace(
                        "Acknowledge Test Workspace",
                        "ack-test-" + UUID.randomUUID()
                )
        );

        Monitor monitor = monitorRepository.save(
                new Monitor(
                        workspace,
                        "Acknowledge Test Monitor",
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
        long auditCountBefore = auditLogRepository.count();

        // Act
        incidentService.acknowledge(incidentId);

        // Assert: incident transitioned OPEN → ACK
        Incident result = incidentRepository.findById(incidentId)
                .orElseThrow();

        assertEquals(IncidentStatus.ACK, result.getStatus());
        assertNotNull(result.getAcknowledgedAt());

        // Assert: audit was created
        assertEquals(
                auditCountBefore + 1,
                auditLogRepository.count()
        );
    }
}