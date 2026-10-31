package com.chandu.roost.dto;

import java.time.Instant;

/**
 * Response view of a subscription. Deliberately omits the secret — it is
 * write-only; once set, Roost never echoes it back.
 */
public record WebhookSubscriptionResponse(
        Long id,
        String url,
        boolean active,
        Instant createdAt
) {
}
