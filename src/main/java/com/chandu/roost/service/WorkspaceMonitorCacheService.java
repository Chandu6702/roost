package com.chandu.roost.service;

import com.chandu.roost.entity.Monitor;
import com.chandu.roost.repository.MonitorRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.chandu.roost.dto.MonitorCacheDto;

import java.util.List;

@Service
public class WorkspaceMonitorCacheService {

    private final MonitorRepository monitorRepository;

    public WorkspaceMonitorCacheService(
            MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }


    @Cacheable(
            cacheNames = "workspaceMonitors",
            key = "#workspaceId"
    )
    public List<MonitorCacheDto> getMonitorsByWorkspaceId(Long workspaceId) {
        return monitorRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(MonitorCacheDto::from)
                .toList();
    }

}