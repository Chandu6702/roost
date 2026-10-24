
package com.chandu.roost.controller;

import com.chandu.roost.dto.MonitorDetailResponse;
import com.chandu.roost.dto.MonitorRequest;
import com.chandu.roost.dto.MonitorResponse;
import com.chandu.roost.dto.ProbeResultResponse;
import com.chandu.roost.entity.Monitor;
import com.chandu.roost.entity.MonitorType;
import com.chandu.roost.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.chandu.roost.dto.MonitorCacheDto;

import java.util.List;

@RestController
@RequestMapping("/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    public ResponseEntity<Page<MonitorResponse>> getMonitors(
            Pageable pageable,
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(required = false) MonitorType type,
            Authentication authentication) {

        Page<MonitorResponse> responses =
                monitorService.getMonitors(
                        pageable,
                        workspaceId,
                        type,
                        authentication.getName()
                ).map(this::toResponse);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<MonitorResponse>> getMonitorsByWorkspace(
            @PathVariable Long workspaceId,
            Authentication authentication) {

        List<MonitorResponse> responses =
                monitorService.getMonitorsByWorkspace(
                                workspaceId,
                                authentication.getName())
                        .stream()
                        .map(dto -> new MonitorResponse(
                                dto.id(),
                                workspaceId,
                                dto.name(),
                                dto.url(),
                                dto.intervalSeconds(),
                                dto.timeoutMillis(),
                                dto.type(),
                                dto.createdAt()
                        ))
                        .toList();

        return ResponseEntity.ok(responses);
    }


    @GetMapping("/workspace/{workspaceId}/search")
    public ResponseEntity<List<MonitorResponse>> searchMonitors(
            @PathVariable Long workspaceId,
            @RequestParam String q,
            Authentication authentication) {

        List<MonitorResponse> responses =
                monitorService.searchMonitors(
                                workspaceId,
                                q,
                                authentication.getName())
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorDetailResponse> getMonitor(
            @PathVariable Long id,
            Pageable pageable,
            Authentication authentication) {

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "probedAt")
            );
        }

        String email = authentication.getName();
        Monitor monitor = monitorService.getMonitor(id, email);

        Page<ProbeResultResponse> probeResults =
                monitorService.getProbeResults(id, pageable, email);

        MonitorDetailResponse response = new MonitorDetailResponse(
                monitor.getId(),
                monitor.getWorkspace().getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getIntervalSeconds(),
                monitor.getTimeoutMillis(),
                monitor.getType(),
                monitor.getCreatedAt(),
                probeResults
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @Valid @RequestBody MonitorRequest request,
            Authentication authentication) {

        Monitor monitor = monitorService.createMonitor(
                request, authentication.getName());

        return ResponseEntity.status(201).body(toResponse(monitor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request,
            Authentication authentication) {

        Monitor monitor = monitorService.updateMonitor(
                id, request, authentication.getName());

        return ResponseEntity.ok(toResponse(monitor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(
            @PathVariable Long id,
            Authentication authentication) {

        monitorService.deleteMonitor(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    private MonitorResponse toResponse(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getWorkspace().getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getIntervalSeconds(),
                monitor.getTimeoutMillis(),
                monitor.getType(),
                monitor.getCreatedAt()
        );
    }
}