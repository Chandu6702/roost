package com.chandu.roost.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global webhook-delivery tuning. Endpoints are no longer configured here —
 * they live per-tenant in {@code webhook_subscriptions}. This holds only the
 * cross-cutting delivery policy.
 */
@ConfigurationProperties(prefix = "roost.webhook")
public record WebhookProperties(
        boolean enabled,
        long timeoutMs,
        int maxAttempts
) {
    public WebhookProperties {
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
    }
}
