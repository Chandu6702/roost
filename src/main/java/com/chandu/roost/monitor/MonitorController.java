package com.chandu.roost.monitor;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            Pageable pageable) {

        Page<MonitorResponse> responses = monitorService.getMonitors(pageable)
                .map(monitor -> new MonitorResponse(
                        monitor.getId(),
                        monitor.getWorkspace().getId(),
                        monitor.getName(),
                        monitor.getUrl(),
                        monitor.getIntervalSeconds(),
                        monitor.getTimeoutMillis(),
                        monitor.getType(),
                        monitor.getCreatedAt()
                ));

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<MonitorResponse>> getMonitorsByWorkspace(
            @PathVariable Long workspaceId) {

        List<MonitorResponse> responses =
                monitorService.getMonitorsByWorkspace(workspaceId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/workspace/{workspaceId}/search")
    public ResponseEntity<List<MonitorResponse>> searchMonitors(
            @PathVariable Long workspaceId,
            @RequestParam String q) {

        List<MonitorResponse> responses =
                monitorService.searchMonitors(workspaceId, q)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitor(@PathVariable Long id) {

        Monitor monitor = monitorService.getMonitor(id);

        MonitorResponse response = toResponse(monitor);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @Valid @RequestBody MonitorRequest request) {

        Monitor monitor = monitorService.createMonitor(request);

        MonitorResponse response =  toResponse(monitor);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request) {

        Monitor monitor = monitorService.updateMonitor(id, request);

        MonitorResponse response = toResponse(monitor);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {

        monitorService.deleteMonitor(id);

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