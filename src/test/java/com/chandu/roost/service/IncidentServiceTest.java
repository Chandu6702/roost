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
        Workspace workspace = workspaceRepository.save(
                new Workspace("Test Workspace", "test-workspace")
        );

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

        // Act
        assertThrows(
                RuntimeException.class,
                () -> incidentService.resolve(incidentId)
        );

        // Assert: outer transaction rolled back
        Incident result = incidentRepository.findById(incidentId)
                .orElseThrow();

        assertEquals(IncidentStatus.OPEN, result.getStatus());
        assertNull(result.getResolvedAt());

        // Assert: REQUIRES_NEW transaction survived
        assertEquals(1, auditLogRepository.count());
    }
}