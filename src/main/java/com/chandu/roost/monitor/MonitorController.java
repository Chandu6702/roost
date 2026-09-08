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
    public ResponseEntity<List<Monitor>> getMonitors() {
        return monitorService.getMonitors();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Monitor> getMonitor(@PathVariable Long id) {
        return monitorService.getMonitor(id);
    }

    @PostMapping
    public ResponseEntity<Monitor> createMonitor(
            @RequestBody Monitor monitor) {

        return monitorService.createMonitor(
                monitor.name(),
                monitor.url(),
                monitor.status()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Monitor> updateMonitor(
            @PathVariable Long id,
            @RequestBody Monitor monitor) {

        return monitorService.updateMonitor(
                id,
                monitor.name(),
                monitor.url(),
                monitor.status()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        return monitorService.deleteMonitor(id);
    }
}