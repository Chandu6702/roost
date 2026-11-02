package com.chandu.roost.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig
@Import(RateLimiterIntegrationTest.RateLimitTestConfig.class)
class RateLimiterIntegrationTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Test
    void allowsUpToLimitThenBlocksWithRetryAfter() {
        String key = "ratelimit:test:" + UUID.randomUUID();
        int limit = 3;
        int window = 60;

        // The first `limit` hits are allowed, each reporting shrinking headroom.
        for (int i = 1; i <= limit; i++) {
            RateLimiter.Decision d = rateLimiter.check(key, limit, window);
            assertTrue(d.allowed(), "hit " + i + " should be allowed");
            assertEquals(limit - i, d.remaining());
        }

        // The next hit is over the limit → blocked with a positive Retry-After.
        RateLimiter.Decision blocked = rateLimiter.check(key, limit, window);
        assertFalse(blocked.allowed());
        assertEquals(0, blocked.remaining());
        assertTrue(blocked.retryAfterSeconds() > 0
                && blocked.retryAfterSeconds() <= window);
    }

    @TestConfiguration
    static class RateLimitTestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory("localhost", 6380);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
            return new StringRedisTemplate(factory);
        }

        @Bean
        RateLimiter rateLimiter(StringRedisTemplate redis) {
            return new RateLimiter(redis);
        }
    }
}
