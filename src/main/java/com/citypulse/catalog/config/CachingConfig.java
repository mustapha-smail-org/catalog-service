package com.citypulse.catalog.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configures the in-process (Caffeine) cache for the catalog read path.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 100)
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CachingConfig {

    public static final String EVENTS = "events";
    public static final String EVENTS_MAP = "eventsMap";
    public static final String FACETS = "facets";
    public static final String CATEGORIES = "categories";
    public static final String EVENT_BY_ID = "eventById";
    public static final String EVENT_BY_SLUG = "eventBySlug";

    /** Every cache name, for {@code @CacheEvict(allEntries = true)} on ingest. */
    public static final String[] ALL = {
            EVENTS, EVENTS_MAP, FACETS, CATEGORIES, EVENT_BY_ID, EVENT_BY_SLUG
    };

    @Bean
    public CacheManager cacheManager(CacheProperties properties) {
        CaffeineCacheManager manager = new CaffeineCacheManager(ALL);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(properties.maxSize())
                .expireAfterWrite(properties.ttl())
                .recordStats());
        return manager;
    }
}
