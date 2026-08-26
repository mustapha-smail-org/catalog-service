package com.citypulse.catalog.enrichment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnrichmentRankScorerTest {

    @Test
    void topScoresMapToOne() {
        assertThat(EnrichmentRankScorer.score(100, 100)).isEqualTo(1.0);
    }

    @Test
    void bottomScoresMapToZero() {
        assertThat(EnrichmentRankScorer.score(0, 0)).isEqualTo(0.0);
    }

    @Test
    void qualityIsWeightedHigherThanUniqueness() {
        double highQuality = EnrichmentRankScorer.score(0, 100);
        double highUniqueness = EnrichmentRankScorer.score(100, 0);
        assertThat(highQuality).isEqualTo(0.6, within(1e-9));
        assertThat(highUniqueness).isEqualTo(0.4, within(1e-9));
        assertThat(highQuality).isGreaterThan(highUniqueness);
    }

    @Test
    void blendsBothComponents() {
        assertThat(EnrichmentRankScorer.score(60, 70))
                .isEqualTo(0.4 * 0.6 + 0.6 * 0.7, within(1e-9));
    }

    @Test
    void clampsOutOfRangeInputs() {
        assertThat(EnrichmentRankScorer.score(200, -50)).isEqualTo(0.4);
    }
}
