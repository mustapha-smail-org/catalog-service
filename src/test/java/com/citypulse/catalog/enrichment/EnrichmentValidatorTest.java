package com.citypulse.catalog.enrichment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentValidatorTest {

    private final EnrichmentValidator validator = new EnrichmentValidator();

    private EnrichmentResult valid() {
        return new EnrichmentResult(
                List.of("CONCERT", "CLUBBING"), List.of("FESTIF"),
                List.of("ENTRE_AMIS"), List.of("techno", "rooftop"),
                "INTENSE", null, 60, 72);
    }

    @Test
    void acceptsAWellFormedResult() {
        assertThat(validator.isValid(valid())).isTrue();
    }

    @Test
    void rejectsUnknownCategory() {
        EnrichmentResult result = new EnrichmentResult(
                List.of("BOGUS"), List.of("FESTIF"), List.of("SOLO"),
                List.of(), "CALME", null, 10, 10);
        assertThat(validator.validate(result))
                .anyMatch(error -> error.contains("categories has unknown value"));
    }

    @Test
    void rejectsTooManyCategories() {
        EnrichmentResult result = new EnrichmentResult(
                List.of("CONCERT", "CLUBBING", "THEATRE", "DANSE", "CINEMA"),
                List.of("FESTIF"), List.of("SOLO"), List.of(), "CALME", null, 10, 10);
        assertThat(validator.validate(result))
                .anyMatch(error -> error.contains("categories must hold"));
    }

    @Test
    void rejectsOutOfRangeScoreAndBadEnergy() {
        EnrichmentResult result = new EnrichmentResult(
                List.of("CONCERT"), List.of("FESTIF"), List.of("SOLO"),
                List.of(), "LOUD", null, 140, -1);
        List<String> errors = validator.validate(result);
        assertThat(errors).anyMatch(e -> e.contains("energyLevel"));
        assertThat(errors).anyMatch(e -> e.contains("uniquenessScore"));
        assertThat(errors).anyMatch(e -> e.contains("qualityScore"));
    }

    @Test
    void rejectsNullResult() {
        assertThat(validator.validate(null)).containsExactly("result is null");
        assertThat(validator.isValid(null)).isFalse();
    }

    @Test
    void rejectsTooManySemanticTags() {
        EnrichmentResult result = new EnrichmentResult(
                List.of("CONCERT"), List.of("FESTIF"), List.of("SOLO"),
                List.of("a", "b", "c", "d", "e", "f", "g", "h", "i"),
                "CALME", null, 10, 10);
        assertThat(validator.validate(result))
                .anyMatch(error -> error.contains("semanticTags"));
    }

    @Test
    void rejectsBadEnvironmentFallback() {
        EnrichmentResult result = new EnrichmentResult(
                List.of("CONCERT"), List.of("FESTIF"), List.of("SOLO"),
                List.of(), "CALME", "UNKNOWN", 10, 10);
        assertThat(validator.validate(result))
                .anyMatch(error -> error.contains("environmentFallback"));
    }
}
