package rw.ac.auca.scoutpro_27202.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

// Ranking, sports and criteria are read often and change rarely, so they are cached in Redis
// for 10 minutes. Writes evict the cache (see @CacheEvict on the service methods).
// If Redis is stopped, the error handler logs the problem and the request still hits PostgreSQL.
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // The JSON in Redis must name the Java class. Without that, a PageResponse
        // comes back as a LinkedHashMap and the cast on the next request fails.
        // NON_FINAL_AND_RECORDS is required because PageResponse is a record, and records are final.
        // The cache is written only by this application, never by a browser.
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .customize(builder -> builder.activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                        DefaultTyping.NON_FINAL_AND_RECORDS,
                        JsonTypeInfo.As.PROPERTY))
                .build();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .prefixCacheNameWith("scoutpro:v2:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis read failed for {} ({}). Using the database.", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis write failed for {} ({}).", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis evict failed for {} ({}).", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis clear failed for {} ({}).", cache.getName(), exception.getMessage());
            }
        };
    }
}
