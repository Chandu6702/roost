package com.chandu.roost.dto;

import com.chandu.roost.entity.IncidentStatus;

import java.time.Instant;

/**
 * Authenticated view of an incident for the dashboard. Workspace-scoped —
 * only ever built from monitors the caller's workspace owns.
 */
public record IncidentResponse(
        Long id,
        Long monitorId,
        String monitorName,
        IncidentStatus status,
        Instant openedAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        String summary
) {
}
