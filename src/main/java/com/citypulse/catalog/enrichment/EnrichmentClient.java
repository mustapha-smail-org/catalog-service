package com.citypulse.catalog.enrichment;

/**
 * Produces enrichment for one event. The real implementation (Spring AI, Track
 * B4) calls an LLM; tests use a deterministic fake. Implementations may throw to
 * signal a transient failure — the worker retries and, failing that, leaves the
 * event unenriched for a later cycle.
 */
public interface EnrichmentClient {

    EnrichmentResult enrich(EnrichmentInput input);
}
