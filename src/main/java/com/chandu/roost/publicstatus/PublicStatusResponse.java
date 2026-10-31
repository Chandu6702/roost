package com.chandu.roost.publicstatus;

import java.time.Instant;
import java.util.List;

/**
 * Public, unauthenticated view of a tenant's status page. Contains only
 * published monitors and their incidents — never internal URLs, IDs of
 * unpublished monitors, or anything tenant-private.
 */
public record PublicStatusResponse(
        String workspace,
        String overall,
        Instant generatedAt,
        List<MonitorStatus> monitors,
        List<RecentIncident> recentIncidents
) {
    public record MonitorStatus(
            String name,
            String status,     // UP / DOWN / UNKNOWN
            Instant lastCheckedAt
    ) {
    }

    public record RecentIncident(
            String monitor,
            String status,
            Instant openedAt,
            Instant resolvedAt
    ) {
    }
}
