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
    public long getTotalMonitors() {
        return monitors.size();
    }
    public List<Monitor> getMonitors(int page, int size, String sort) {
        List<Monitor> allMonitors = new ArrayList<>(monitors.values());

        String[]  sortParts = sort.split(",");
        String field = sortParts[0];
        String direction = sortParts.length > 1 ? sortParts[1] : "asc";
        if("name".equals(field)){
            allMonitors.sort((a, b) ->"desc".equalsIgnoreCase(direction) ? b.getName().compareToIgnoreCase(a.getName()) : a.getName().compareToIgnoreCase(b.getName()));
        }
        int start = page * size;
        int end = Math.min(start + size, allMonitors.size());

        if(start >= allMonitors.size()) {
            return List.of();
        }

        return allMonitors.subList(start, end);
    }

    public Monitor getMonitor(Long id) {
        Monitor monitor = monitors.get(id);

        if (monitor == null) {
            throw new MonitorNotFoundException(id);
        }

        return monitor;
    }

    public Monitor createMonitor(MonitorRequest request) {
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

        if (!monitors.containsKey(id)) {
            throw new MonitorNotFoundException(id);
        }

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

        if (!monitors.containsKey(id)) {
            throw new MonitorNotFoundException(id);
        }

        monitors.remove(id);
    }
}