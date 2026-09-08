package com.chandu.roost.monitor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitorService {

    private final Map<Long, Monitor> monitors = new HashMap<>();
    private long nextId = 1;

    public ResponseEntity<List<Monitor>> getMonitors() {
        return ResponseEntity.ok(new ArrayList<>(monitors.values()));
    }

    public ResponseEntity<Monitor> getMonitor(Long id) {
        Monitor monitor = monitors.get(id);

        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(monitor);
    }

    public ResponseEntity<Monitor> createMonitor(
            String name,
            String url,
            String status) {

        Long id = nextId++;

        Monitor newMonitor = new Monitor(
                id,
                name,
                url,
                status
        );

        monitors.put(id, newMonitor);

        return ResponseEntity.status(201).body(newMonitor);
    }

    public ResponseEntity<Monitor> updateMonitor(
            Long id,
            String name,
            String url,
            String status) {

        if (!monitors.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }

        Monitor updatedMonitor = new Monitor(
                id,
                name,
                url,
                status
        );

        monitors.put(id, updatedMonitor);

        return ResponseEntity.ok(updatedMonitor);
    }

    public ResponseEntity<Void> deleteMonitor(Long id) {

        if (!monitors.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }

        monitors.remove(id);

        return ResponseEntity.noContent().build();
    }
}