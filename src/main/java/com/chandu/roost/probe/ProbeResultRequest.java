package com.chandu.roost.probe;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ProbeResultRequest(
        @NotNull
        Instant probedAt,

        @NotBlank
        String status,

        @NotNull
        @Min(0)
        Integer latencyMs,

        String errorMessage
) {
}
