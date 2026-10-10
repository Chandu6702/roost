
package com.chandu.roost.service;

import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.dto.ProbeResultResponse;
import com.chandu.roost.entity.*;
import com.chandu.roost.error.MonitorNotFoundException;
import com.chandu.roost.repository.MonitorRepository;
import com.chandu.roost.repository.ProbeResultRepository;
import com.chandu.roost.repository.ProbeScheduleRepository;
import com.chandu.roost.repository.WorkspaceRepository;
import com.chandu.roost.specification.MonitorSpecification;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProbeResultRepository probeResultRepository;
    private final ProbeScheduleRepository probeScheduleRepository;
    private final AuditService auditService;
    private final CurrentWorkspaceService currentWorkspaceService;
    private final EntityManager entityManager;

    public MonitorService(
            MonitorRepository monitorRepository,
            WorkspaceRepository workspaceRepository,
            ProbeResultRepository probeResultRepository,
            ProbeScheduleRepository probeScheduleRepository,
            AuditService auditService,
            CurrentWorkspaceService currentWorkspaceService,
            EntityManager entityManager) {

        this.monitorRepository = monitorRepository;
        this.workspaceRepository = workspaceRepository;
        this.probeResultRepository = probeResultRepository;
        this.probeScheduleRepository = probeScheduleRepository;
        this.auditService = auditService;
        this.currentWorkspaceService = currentWorkspaceService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public long getTotalMonitors(String email) {
        Long workspaceId = tenantWorkspaceId(email);
        return monitorRepository.countByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<Monitor> getMonitors(String email) {
        Long workspaceId = tenantWorkspaceId(email);
        return monitorRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public Monitor getMonitor(Long id, String email) {
        Long workspaceId = tenantWorkspaceId(email);

        return monitorRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new MonitorNotFoundException(id));
    }

    @Transactional
    public Monitor createMonitor(
            MonitorRequest request, String email) {

        // Never use request.workspaceId() to establish ownership.
        Long workspaceId = tenantWorkspaceId(email);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));

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

        probeScheduleRepository.save(
                new ProbeSchedule(savedMonitor, Instant.now()));

        auditService.logMonitorEvent(savedMonitor, "MONITOR_CREATED");

        return savedMonitor;
    }

    @Transactional
    public Monitor updateMonitor(
            Long id, MonitorRequest request, String email) {

        // Only loads a monitor in the current user's workspace.
        Monitor monitor = getMonitor(id, email);

        // Deliberately do not change monitor.workspace.
        monitor.setName(request.name());
        monitor.setUrl(request.url());
        monitor.setType(request.type());
        monitor.setIntervalSeconds(request.intervalSeconds());
        monitor.setTimeoutMillis(request.timeoutMillis());

        return monitorRepository.save(monitor);
    }

    @Transactional
    public void deleteMonitor(Long id, String email) {
        Monitor monitor = getMonitor(id, email);
        monitorRepository.delete(monitor);
    }

    @Transactional(readOnly = true)
    public List<Monitor> getMonitorsByWorkspace(
            Long requestedWorkspaceId, String email) {

        Long workspaceId = tenantWorkspaceId(email);
        requireSameWorkspace(requestedWorkspaceId, workspaceId);

        return monitorRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<Monitor> searchMonitors(
            Long requestedWorkspaceId, String q, String email) {

        Long workspaceId = tenantWorkspaceId(email);
        requireSameWorkspace(requestedWorkspaceId, workspaceId);

        return monitorRepository
                .findByWorkspaceIdAndNameContainingIgnoreCase(
                        workspaceId, q);
    }

    @Transactional(readOnly = true)
    public Page<Monitor> getMonitors(
            Pageable pageable,
            Long requestedWorkspaceId,
            MonitorType type,
            String email) {

        Long workspaceId = tenantWorkspaceId(email);

        if (requestedWorkspaceId != null) {
            requireSameWorkspace(requestedWorkspaceId, workspaceId);
        }

        Specification<Monitor> specification =
                Specification.allOf(
                        MonitorSpecification.belongsToWorkspace(workspaceId),
                        MonitorSpecification.hasType(type)
                );

        return monitorRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProbeResultResponse> getProbeResults(
            Long monitorId, Pageable pageable, String email) {

        // Ownership check happens before querying the child records.
        getMonitor(monitorId, email);

        return probeResultRepository.findByMonitorId(monitorId, pageable)
                .map(result -> new ProbeResultResponse(
                        result.getId(),
                        result.getProbedAt(),
                        result.getStatus(),
                        result.getLatencyMs(),
                        result.getErrorMessage()
                ));
    }

    public double calculateUptimePercent(List<ProbeResult> probeResults) {
        if (probeResults.isEmpty()) {
            return 0.0;
        }

        long upCount = probeResults.stream()
                .filter(result -> "UP".equals(result.getStatus()))
                .count();

        return upCount * 100.0 / probeResults.size();
    }

    private Long tenantWorkspaceId(String email) {
        Long workspaceId =
                currentWorkspaceService.getCurrentWorkspaceId(email);

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.getEnabledFilter("workspaceFilter");

        if (filter == null) {
            session.enableFilter("workspaceFilter")
                    .setParameter("workspaceId", workspaceId);
        } else {
            filter.setParameter("workspaceId", workspaceId);
        }

        return workspaceId;
    }

    private void requireSameWorkspace(
            Long requestedWorkspaceId, Long currentWorkspaceId) {

        if (requestedWorkspaceId == null
                || !requestedWorkspaceId.equals(currentWorkspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }
    }
}