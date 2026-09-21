package com.chandu.roost.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record MonitorRequest(
        @NotNull
        Long workspaceId,

        @NotBlank
        @Schema(description = "The name of the monitor")
        String name,

        @NotBlank
        @URL
        String url,

        @Min(30)
        @Max(3600)
        @JsonProperty("interval_seconds")
        Integer intervalSeconds,

        @Min(100)
        @Max(60000)
        @JsonProperty("timeout_millis")
        Integer timeoutMillis,

        @NotNull
        MonitorType type
) {
}