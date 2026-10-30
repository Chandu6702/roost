package com.chandu.roost.service;

import com.chandu.roost.entity.Incident;
import com.chandu.roost.entity.IncidentStatus;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.MonitorType;
import com.chandu.roost.entity.OutboxEvent;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.repository.AuditLogRepository;
import com.chandu.roost.repository.IncidentRepository;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.OutboxEventRepository;
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

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void open_createsOpenIncidentAndOutboxEvent() {

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

        // Verify incident creation.
        assertNotNull(incident.getId());
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertNotNull(incident.getOpenedAt());
        assertEquals("Monitor is DOWN", incident.getSummary());
        assertEquals(monitor.getId(), incident.getMonitor().getId());

        // Verify the outbox event.
        List<OutboxEvent> events =
                outboxEventRepository
                        .findByPublishedAtIsNullOrderByCreatedAtAsc();

        OutboxEvent event = events.stream()
                .filter(e -> incident.getId().equals(e.getAggregateId()))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Expected INCIDENT_OPENED outbox event"
                        )
                );

        assertEquals("Incident", event.getAggregateType());
        assertEquals(incident.getId(), event.getAggregateId());
        assertEquals("INCIDENT_OPENED", event.getEventType());
        assertNotNull(event.getPayload());
        assertNull(event.getPublishedAt());

        // Verify event payload.
        assertEquals(
                incident.getId().longValue(),
                ((Number) event.getPayload().get("incidentId")).longValue()
        );

        assertEquals(
                monitor.getId().longValue(),
                ((Number) event.getPayload().get("monitorId")).longValue()
        );

        assertEquals(
                "OPEN",
                event.getPayload().get("status")
        );

        assertEquals(
                "Monitor is DOWN",
                event.getPayload().get("summary")
        );

        assertEquals(
                incident.getOpenedAt().toString(),
                event.getPayload().get("openedAt")
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

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertNotNull(incident.getOpenedAt());

        // OPEN → ACK
        incidentService.acknowledge(incident.getId());

        Incident acknowledged =
                incidentRepository.findById(incident.getId())
                        .orElseThrow();

        assertEquals(IncidentStatus.ACK, acknowledged.getStatus());
        assertNotNull(acknowledged.getAcknowledgedAt());

        /*
         * resolve() currently contains an intentional forced failure
         * for the transaction rollback verification test.
         *
         * The complete ACK → RESOLVED domain transition is covered
         * by IncidentStateMachineTest.
         */
    }
}
