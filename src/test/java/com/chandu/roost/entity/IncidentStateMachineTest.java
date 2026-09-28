package com.chandu.roost.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class IncidentStateMachineTest {

    private Incident openIncident() {
        return new Incident(
                null,
                IncidentStatus.OPEN,
                Instant.now(),
                "Test incident"
        );
    }

    private Incident acknowledgedIncident() {
        Incident incident = openIncident();
        incident.acknowledge();
        return incident;
    }

    private Incident resolvedIncident() {
        Incident incident = acknowledgedIncident();
        incident.resolve();
        return incident;
    }

    @Test
    void openIncident_canBeAcknowledged() {
        Incident incident = openIncident();

        incident.acknowledge();

        assertEquals(IncidentStatus.ACK, incident.getStatus());
        assertNotNull(incident.getAcknowledgedAt());
    }

    @Test
    void acknowledgedIncident_canBeResolved() {
        Incident incident = acknowledgedIncident();

        incident.resolve();

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertNotNull(incident.getResolvedAt());
    }

    @Test
    void openIncident_cannotBeResolvedDirectly() {
        Incident incident = openIncident();

        assertThrows(
                IllegalStateException.class,
                incident::resolve
        );

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertNull(incident.getResolvedAt());
    }

    @Test
    void acknowledgedIncident_cannotBeAcknowledgedAgain() {
        Incident incident = acknowledgedIncident();

        assertThrows(
                IllegalStateException.class,
                incident::acknowledge
        );

        assertEquals(IncidentStatus.ACK, incident.getStatus());
    }

    @Test
    void resolvedIncident_cannotBeAcknowledged() {
        Incident incident = resolvedIncident();

        assertThrows(
                IllegalStateException.class,
                incident::acknowledge
        );

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    void resolvedIncident_cannotBeResolvedAgain() {
        Incident incident = resolvedIncident();

        assertThrows(
                IllegalStateException.class,
                incident::resolve
        );

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    void incident_followsCompleteLifecycle() {
        Incident incident = openIncident();

        assertEquals(IncidentStatus.OPEN, incident.getStatus());

        incident.acknowledge();

        assertEquals(IncidentStatus.ACK, incident.getStatus());
        assertNotNull(incident.getAcknowledgedAt());

        incident.resolve();

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertNotNull(incident.getResolvedAt());
    }
}