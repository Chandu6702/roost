package com.chandu.roost.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roost.webhook")
public record WebhookProperties(
        boolean enabled,
        String endpointUrl,
        long timeoutMs
) {
}