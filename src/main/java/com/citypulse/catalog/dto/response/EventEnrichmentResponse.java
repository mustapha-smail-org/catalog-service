package com.citypulse.catalog.dto.response;

import java.util.List;

/**
 * AI-derived enrichment for an event. Present only once the enrichment worker
 * has processed the event; the surrounding response nulls this block while the
 * event is still unenriched.
 */
public record EventEnrichmentResponse(
        List<String> categories,
        List<String> moodAffinities,
        List<String> socialContexts,
        List<String> semanticTags,
        String energyLevel,
        String environmentFallback,
        Integer uniquenessScore,
        Integer qualityScore,
        Double rankScore
) {
}
