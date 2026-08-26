package com.citypulse.catalog.enrichment;

import java.util.List;

/**
 * The enrichment client's output for one event — mirrors the frozen enrichment
 * schema. Validated before it is persisted (see {@link EnrichmentValidator}).
 */
public record EnrichmentResult(
        List<String> categories,
        List<String> moodAffinities,
        List<String> socialContexts,
        List<String> semanticTags,
        String energyLevel,
        String environmentFallback,
        Integer uniquenessScore,
        Integer qualityScore
) {
}
