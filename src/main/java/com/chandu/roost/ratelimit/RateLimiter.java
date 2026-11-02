package com.chandu.roost.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Fixed-window rate limiter backed by Redis ({@code INCR} then {@code EXPIRE}).
 *
 * <p>The counter lives in Redis, not in the JVM, so every app instance counts
 * against the <b>same</b> window per key — the only correct choice once the
 * service runs horizontally (an in-memory limiter would hand each instance the
 * full quota, multiplying the real limit by the instance count).
 *
 * <p><b>Fails open:</b> if Redis is unreachable we allow the request rather than
 * lock everyone out of login. A limiter should shed its own load, never become a
 * single point of failure for authentication.
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Records one hit against {@code key} and decides whether it is allowed.
     *
     * @param key           the bucket (e.g. {@code ratelimit:login:<ip>})
     * @param limit         max hits permitted per window
     * @param windowSeconds window length in seconds
     */
    public Decision check(String key, int limit, int windowSeconds) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return new Decision(true, limit, limit, 0);
            }
            if (count == 1L) {
                // First hit in this window — start the TTL clock so the counter
                // resets automatically when the window elapses.
                redis.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count > limit) {
                Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
                long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;
                return new Decision(false, limit, 0, retryAfter);
            }
            return new Decision(true, limit, Math.max(0, limit - count), 0);
        } catch (DataAccessException e) {
            log.warn("Rate limiter unavailable ({}); allowing request", e.getMessage());
            return new Decision(true, limit, limit, 0);
        }
    }

    public record Decision(
            boolean allowed,
            long limit,
            long remaining,
            long retryAfterSeconds) {
    }
}
