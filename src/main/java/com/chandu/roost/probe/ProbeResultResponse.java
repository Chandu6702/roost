package com.chandu.roost.probe;

import java.time.Instant;

public record ProbeResultResponse(
        Long id,
        Instant probedAt,
        String status,
        Integer latencyMs,
        String errorMessage
) {
}
