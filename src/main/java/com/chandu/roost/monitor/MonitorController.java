package com.chandu.roost.monitor;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/monitors")
@Tag(name = "Monitors", description = "Monitor management APIs")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    @Operation(summary = "Get all monitors", description = "Retrieve a list of all monitors")
    public ResponseEntity<MonitorPageResponse> getMonitors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {

        List<MonitorResponse> responses = monitorService.getMonitors(page, size, sort)
                .stream()
                .map(monitor -> new MonitorResponse(
                        monitor.getId(),
                        monitor.getName(),
                        monitor.getUrl(),
                        monitor.getStatus(),
                        monitor.getCheckIntervalSeconds(),
                        monitor.getType()
                ))
                .toList();
        long totalElements = monitorService.getTotalMonitors();
        MonitorPageResponse response = new MonitorPageResponse(responses, page, size, totalElements);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitor(@PathVariable Long id) {
        Monitor monitor = monitorService.getMonitor(id);
        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }
        MonitorResponse response = new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getStatus(),
                monitor.getCheckIntervalSeconds(),
                monitor.getType()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @Valid @RequestBody MonitorRequest request) {
        Monitor monitor = monitorService.createMonitor(request);
        MonitorResponse response = new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getStatus(),
                monitor.getCheckIntervalSeconds(),
                monitor.getType()
        );
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request) {

        Monitor monitor = monitorService.updateMonitor(id, request);
        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }
        MonitorResponse response = new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getStatus(),
                monitor.getCheckIntervalSeconds(),
                monitor.getType()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
         monitorService.deleteMonitor(id);
         return ResponseEntity.noContent().build();
    }
}