package com.chandu.roost.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import io.swagger.v3.oas.annotations.media.Schema;

public record MonitorRequest(
    @NotBlank
    @Schema(description = "The name of the monitor")
    String name,
    @NotBlank @URL
    String url,
    String status,
    @Min(30) @Max(3600) @JsonProperty("check_interval_seconds")
    Integer checkIntervalSeconds,
    @NotNull
    MonitorType type
) {
}