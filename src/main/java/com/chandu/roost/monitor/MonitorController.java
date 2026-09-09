package com.chandu.roost.monitor;

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
    public ResponseEntity<List<MonitorResponse>> getMonitors() {
        List<MonitorResponse> responses = monitorService.getMonitors()
                .stream()
                .map(monitor -> new MonitorResponse(
                        monitor.getId(),
                        monitor.getName(),
                        monitor.getUrl(),
                        monitor.getStatus(),
                        monitor.getCheckIntervalSeconds()
                ))
                .toList();
        return ResponseEntity.ok(responses);
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
                monitor.getCheckIntervalSeconds()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @RequestBody MonitorRequest request) {
        Monitor monitor = monitorService.createMonitor(request);
        MonitorResponse response = new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getStatus(),
                monitor.getCheckIntervalSeconds()
        );
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @PathVariable Long id,
            @RequestBody MonitorRequest request) {

        Monitor monitor = monitorService.updateMonitor(id, request);
        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }
        MonitorResponse response = new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getStatus(),
                monitor.getCheckIntervalSeconds()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
         monitorService.deleteMonitor(id);
         return ResponseEntity.noContent().build();
    }
}