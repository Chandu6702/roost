
package com.chandu.roost.service;

import com.chandu.roost.dto.MonitorCacheDto;
import com.chandu.roost.repository.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceMonitorCacheService {

    private static final Logger log =
            LoggerFactory.getLogger(WorkspaceMonitorCacheService.class);

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
        log.info("CACHE MISS: loading monitors for workspace {}", workspaceId);

        return monitorRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(MonitorCacheDto::from)
                .toList();
    }
}
