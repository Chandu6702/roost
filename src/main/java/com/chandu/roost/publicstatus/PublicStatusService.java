package com.chandu.roost.publicstatus;

import com.chandu.roost.incident.Incident;
import com.chandu.roost.monitor.Monitor;
import com.chandu.roost.incident.IncidentRepository;
import com.chandu.roost.monitor.MonitorRepository;
import com.chandu.roost.probe.ProbeResultRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Builds the public status page for a single tenant. Every query is explicitly
 * scoped by {@code workspaceId} and restricted to <b>published</b> monitors, so
 * one tenant's page can never reveal another tenant's — or its own private —
 * monitors. This is the integration-tested isolation guarantee.
 */
@Service
public class PublicStatusService {

    private static final int RECENT_INCIDENT_LIMIT = 5;

    private final MonitorRepository monitorRepository;
    private final ProbeResultRepository probeResultRepository;
    private final IncidentRepository incidentRepository;

    public PublicStatusService(
            MonitorRepository monitorRepository,
            ProbeResultRepository probeResultRepository,
            IncidentRepository incidentRepository) {
        this.monitorRepository = monitorRepository;
        this.probeResultRepository = probeResultRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public PublicStatusResponse build(Long workspaceId, String workspaceName) {
        List<Monitor> published =
                monitorRepository.findByWorkspaceIdAndPublishedTrue(workspaceId);

        List<PublicStatusResponse.MonitorStatus> monitorStatuses = published.stream()
                .map(this::toMonitorStatus)
                .toList();

        boolean anyDown = monitorStatuses.stream()
                .anyMatch(m -> "DOWN".equals(m.status()));
        String overall = monitorStatuses.isEmpty() ? "UNKNOWN"
                : anyDown ? "DEGRADED" : "OPERATIONAL";

        List<Long> monitorIds = published.stream().map(Monitor::getId).toList();
        List<PublicStatusResponse.RecentIncident> recent = monitorIds.isEmpty()
                ? List.of()
                : incidentRepository
                        .findByMonitorIdInOrderByOpenedAtDesc(
                                monitorIds, PageRequest.of(0, RECENT_INCIDENT_LIMIT))
                        .stream()
                        .map(this::toRecentIncident)
                        .toList();

        return new PublicStatusResponse(
                workspaceName, overall, Instant.now(), monitorStatuses, recent);
    }

    private PublicStatusResponse.MonitorStatus toMonitorStatus(Monitor monitor) {
        return probeResultRepository
                .findFirstByMonitorIdOrderByProbedAtDesc(monitor.getId())
                .map(r -> new PublicStatusResponse.MonitorStatus(
                        monitor.getName(), r.getStatus(), r.getProbedAt()))
                .orElseGet(() -> new PublicStatusResponse.MonitorStatus(
                        monitor.getName(), "UNKNOWN", null));
    }

    private PublicStatusResponse.RecentIncident toRecentIncident(Incident incident) {
        return new PublicStatusResponse.RecentIncident(
                incident.getMonitor().getName(),
                incident.getStatus().name(),
                incident.getOpenedAt(),
                incident.getResolvedAt());
    }
}
