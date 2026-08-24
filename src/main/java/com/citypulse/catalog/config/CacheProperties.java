package com.citypulse.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tuning for the read-path response cache.
 *
 * @param enabled turns the whole cache layer on/off (see {@link CachingConfig})
 * @param ttl     max staleness; entries expire this long after being written
 * @param maxSize per-cache entry cap (LRU eviction) — bounds unbounded key spaces
 *
 */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        boolean enabled,
        Duration ttl,
        long maxSize
) {

    public CacheProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(10);
        }
        if (maxSize <= 0) {
            maxSize = 1000;
        }
    }
}
