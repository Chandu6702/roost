package com.chandu.roost.monitor;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitorService {

    private final Map<Long, Monitor> monitors = new HashMap<>();
    private long nextId = 1;

    public List<Monitor> getMonitors() {
        return new ArrayList<>(monitors.values());
    }

    public Monitor getMonitor(Long id) {
        Monitor monitor = monitors.get(id);
        return monitor;
    }

    public Monitor createMonitor(
            MonitorRequest request) {

        Long id = nextId++;

        Monitor newMonitor = new Monitor(
                id,
                request.name(),
                request.url(),
                request.status(),
                request.checkIntervalSeconds(),
                request.type()
        );

        monitors.put(id, newMonitor);

        return newMonitor;
    }

    public Monitor updateMonitor(Long id, MonitorRequest request) {
        Monitor updatedMonitor = new Monitor(
                id,
                request.name(),
                request.url(),
                request.status(),
                request.checkIntervalSeconds(),
                request.type()
        );

        monitors.put(id, updatedMonitor);

        return updatedMonitor;
    }

    public void deleteMonitor(Long id) {
        monitors.remove(id);
    }
}