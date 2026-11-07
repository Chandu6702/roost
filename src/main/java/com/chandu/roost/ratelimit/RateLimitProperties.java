package com.chandu.roost.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning for the login rate limiter. Compact constructor supplies safe defaults
 * (5 attempts per 60s) when a property is missing or non-positive.
 */
@ConfigurationProperties(prefix = "roost.ratelimit")
public record RateLimitProperties(int loginLimit, int loginWindowSeconds) {

    public RateLimitProperties {
        if (loginLimit <= 0) {
            loginLimit = 5;
        }
        if (loginWindowSeconds <= 0) {
            loginWindowSeconds = 60;
        }
    }
}
