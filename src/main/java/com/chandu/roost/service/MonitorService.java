package com.chandu.roost.service;

import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.MonitorType;
import com.chandu.roost.entity.ProbeSchedule;
import com.chandu.roost.error.MonitorNotFoundException;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.ProbeResultRepository;
import com.chandu.roost.dto.ProbeResultResponse;
import com.chandu.roost.entity.Workspace;
import com.chandu.roost.repository.ProbeScheduleRepository;
import com.chandu.roost.repository.WorkspaceRepository;
import com.chandu.roost.specification.MonitorSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProbeResultRepository probeResultRepository;
    private final ProbeScheduleRepository probeScheduleRepository;
    private final AuditService auditService;

    public MonitorService(
            MonitorRepository monitorRepository,
            WorkspaceRepository workspaceRepository,
            ProbeResultRepository probeResultRepository,
            ProbeScheduleRepository probeScheduleRepository,
            AuditService auditService) {

        this.monitorRepository = monitorRepository;
        this.workspaceRepository = workspaceRepository;
        this.probeResultRepository = probeResultRepository;
        this.probeScheduleRepository = probeScheduleRepository;
        this.auditService = auditService;
    }

    public long getTotalMonitors() {
        return monitorRepository.count();
    }

    public List<Monitor> getMonitors() {
        return monitorRepository.findAll();
    }

    public Monitor getMonitor(Long id) {
        return monitorRepository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
    }

    @Transactional
    public Monitor createMonitor(MonitorRequest request) {

        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Workspace with id " + request.workspaceId() + " was not found"
                        ));

        Monitor monitor = new Monitor(
                workspace,
                request.name(),
                request.url(),
                request.type(),
                request.intervalSeconds(),
                request.timeoutMillis(),
                Instant.now()
        );

        Monitor savedMonitor = monitorRepository.save(monitor);

        ProbeSchedule schedule = new ProbeSchedule(
                savedMonitor,
                Instant.now()
        );

        probeScheduleRepository.save(schedule);

        auditService.logMonitorEvent(
                savedMonitor,
                "MONITOR_CREATED"
        );

        return savedMonitor;
    }

    public Monitor updateMonitor(Long id, MonitorRequest request) {

        Monitor monitor = getMonitor(id);

        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Workspace with id " + request.workspaceId() + " was not found"
                        ));

        monitor.setWorkspace(workspace);
        monitor.setName(request.name());
        monitor.setUrl(request.url());
        monitor.setType(request.type());
        monitor.setIntervalSeconds(request.intervalSeconds());
        monitor.setTimeoutMillis(request.timeoutMillis());

        return monitorRepository.save(monitor);
    }

    public void deleteMonitor(Long id) {
        Monitor monitor = getMonitor(id);
        monitorRepository.delete(monitor);
    }

    public List<Monitor> getMonitorsByWorkspace(Long workspaceId) {
        return monitorRepository.findByWorkspaceId(workspaceId);
    }

    public List<Monitor> searchMonitors(
            Long workspaceId,
            String q) {

        return monitorRepository
                .findByWorkspaceIdAndNameContainingIgnoreCase(
                        workspaceId,
                        q
                );
    }

    public Page<Monitor> getMonitors(
            Pageable pageable,
            Long workspaceId,
            MonitorType type) {
        Specification<Monitor> specification =
                Specification.allOf(
                        MonitorSpecification.belongsToWorkspace(workspaceId),
                        MonitorSpecification.hasType(type)
                );

        return monitorRepository.findAll(specification, pageable);
    }

    public Page<ProbeResultResponse> getProbeResults(
            Long monitorId,
            Pageable pageable) {

        return probeResultRepository
                .findByMonitorId(monitorId, pageable)
                .map(probeResult -> new ProbeResultResponse(
                        probeResult.getId(),
                        probeResult.getProbedAt(),
                        probeResult.getStatus(),
                        probeResult.getLatencyMs(),
                        probeResult.getErrorMessage()
                ));
    }
}