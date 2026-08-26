package com.citypulse.catalog.enrichment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling only when the enrichment worker is turned on, so nothing
 * fires by default.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.enrichment.enabled", havingValue = "true")
public class EnrichmentConfig {
}
