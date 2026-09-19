package com.chandu.roost.monitor;

import com.chandu.roost.probe.ProbeResultResponse;
import org.springframework.data.domain.Page;

import java.time.Instant;

public record MonitorDetailResponse(
        Long id,
        Long workspaceId,
        String name,
        String url,
        Integer intervalSeconds,
        Integer timeoutMillis,
        MonitorType type,
        Instant createdAt,
        Page<ProbeResultResponse> probeResults) {
}
