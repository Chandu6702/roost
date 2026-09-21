package com.chandu.roost.dto;

import com.chandu.roost.entity.MonitorType;
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
