package com.citypulse.catalog.enrichment;

/**
 * The intrinsic rank score for an enriched event: a stable 0.0–1.0 blend of
 * quality and uniqueness, weighted toward quality (worth-attending matters more
 * than novelty for the default feed). Deliberately time-free — freshness is a
 * live signal applied at query time (Track C1), because a precomputed freshness
 * would go stale between re-enrichments. Recomputed whenever enrichment is
 * (re)written, so it never needs sort-on-read.
 */
public final class EnrichmentRankScorer {

    private static final double UNIQUENESS_WEIGHT = 0.4;
    private static final double QUALITY_WEIGHT = 0.6;

    private EnrichmentRankScorer() {
    }

    public static double score(int uniquenessScore, int qualityScore) {
        double uniqueness = clamp(uniquenessScore) / 100.0;
        double quality = clamp(qualityScore) / 100.0;
        return UNIQUENESS_WEIGHT * uniqueness + QUALITY_WEIGHT * quality;
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
