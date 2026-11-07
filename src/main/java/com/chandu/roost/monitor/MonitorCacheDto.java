
package com.chandu.roost.monitor;


import java.io.Serializable;
import java.time.Instant;

public record MonitorCacheDto(
        Long id,
        String name,
        String url,
        MonitorType type,
        Integer intervalSeconds,
        Integer timeoutMillis,
        Instant createdAt
) implements Serializable {
    public static MonitorCacheDto from(Monitor monitor) {
        return new MonitorCacheDto(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getType(),
                monitor.getIntervalSeconds(),
                monitor.getTimeoutMillis(),
                monitor.getCreatedAt()
        );
    }
}
