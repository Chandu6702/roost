package com.chandu.roost.dto;

import java.time.Instant;

public record ProbeResultResponse(
        Long id,
        Instant probedAt,
        String status,
        Integer latencyMs,
        String errorMessage
) {
}
