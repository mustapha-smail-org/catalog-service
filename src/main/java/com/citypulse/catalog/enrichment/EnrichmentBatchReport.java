package com.citypulse.catalog.enrichment;

/**
 * Outcome of one enrichment cycle. {@code enriched} counts events that produced
 * a valid result (persisted, or would-be-persisted in dry-run); {@code skipped}
 * counts invalid outputs held back; {@code failed} counts events whose client
 * calls kept throwing. Every non-enriched event is simply retried next cycle.
 */
public record EnrichmentBatchReport(
        int selected,
        int enriched,
        int skipped,
        int failed
) {
}
