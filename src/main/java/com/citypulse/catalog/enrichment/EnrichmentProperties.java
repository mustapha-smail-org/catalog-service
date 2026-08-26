package com.citypulse.catalog.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning for the enrichment worker.
 *
 * @param enabled       master switch — the worker and scheduler only wire when true
 * @param dryRun        run + validate but never persist (cost/observability check)
 * @param batchSize     max events picked per cycle
 * @param maxRetries    in-cycle retries on a transient client failure
 * @param promptVersion current prompt version; a bump re-enriches everything
 * @param model         recorded as {@code enrichment_model} on each row
 */
@ConfigurationProperties(prefix = "app.enrichment")
public record EnrichmentProperties(
        boolean enabled,
        boolean dryRun,
        int batchSize,
        int maxRetries,
        int promptVersion,
        String model
) {

    public EnrichmentProperties {
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (maxRetries < 0) {
            maxRetries = 2;
        }
        if (promptVersion <= 0) {
            promptVersion = 1;
        }
        if (model == null || model.isBlank()) {
            model = "stub";
        }
    }
}
