
package com.chandu.roost.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig
@Import(RedisCacheIntegrationTest.CacheTestConfig.class)
class RedisCacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void redisCache_storesAndRetrievesString() {
        Cache cache = cacheManager.getCache("redisTest");

        assertNotNull(cache);

        cache.put("workspace-1", "cached successfully");

        String result = cache.get("workspace-1", String.class);

        assertEquals("cached successfully", result);
    }

    @TestConfiguration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory("localhost", 6380);
        }

        @Bean
        CacheManager cacheManager(
                RedisConnectionFactory redisConnectionFactory) {

            RedisCacheConfiguration configuration =
                    RedisCacheConfiguration.defaultCacheConfig()
                            .serializeKeysWith(
                                    RedisSerializationContext.SerializationPair
                                            .fromSerializer(
                                                    new StringRedisSerializer()
                                            )
                            )
                            .serializeValuesWith(
                                    RedisSerializationContext.SerializationPair
                                            .fromSerializer(
                                                    new StringRedisSerializer()
                                            )
                            );

            return RedisCacheManager.builder(redisConnectionFactory)
                    .withCacheConfiguration("redisTest", configuration)
                    .build();
        }
    }
}
