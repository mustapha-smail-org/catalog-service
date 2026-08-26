package com.citypulse.catalog.enrichment;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires the enrichment worker on a fixed cadence. Off unless
 * {@code app.enrichment.enabled=true}; the delay is deliberately long so the
 * worker never contends with query serving.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.enrichment.enabled", havingValue = "true")
public class EnrichmentScheduler {

    private final EventEnrichmentService service;

    @Scheduled(
            initialDelayString = "${app.enrichment.initial-delay-ms:60000}",
            fixedDelayString = "${app.enrichment.interval-ms:300000}"
    )
    public void run() {
        service.enrichPending();
    }
}
