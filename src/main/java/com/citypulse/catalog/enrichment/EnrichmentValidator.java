package com.citypulse.catalog.enrichment;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Enforces the frozen enrichment schema before anything is persisted: closed
 * vocab membership, array caps, single energy level, and 0–100 scores. Returns
 * every problem found so a rejected result can be logged usefully.
 */
@Component
public class EnrichmentValidator {

    public List<String> validate(EnrichmentResult result) {
        List<String> errors = new ArrayList<>();

        if (result == null) {
            errors.add("result is null");
            return errors;
        }

        checkVocab(errors, "categories", result.categories(),
                EnrichmentVocabulary.CATEGORIES, 1, 4);
        checkVocab(errors, "moodAffinities", result.moodAffinities(),
                EnrichmentVocabulary.MOODS, 1, 3);
        checkVocab(errors, "socialContexts", result.socialContexts(),
                EnrichmentVocabulary.SOCIAL_CONTEXTS, 1, 3);

        if (result.energyLevel() == null
                || !EnrichmentVocabulary.ENERGY_LEVELS.contains(result.energyLevel())) {
            errors.add("energyLevel must be one of " + EnrichmentVocabulary.ENERGY_LEVELS);
        }

        if (result.environmentFallback() != null
                && !EnrichmentVocabulary.ENVIRONMENT_FALLBACKS.contains(result.environmentFallback())) {
            errors.add("environmentFallback must be INDOOR, OUTDOOR or null");
        }

        checkScore(errors, "uniquenessScore", result.uniquenessScore());
        checkScore(errors, "qualityScore", result.qualityScore());

        List<String> tags = result.semanticTags();
        if (tags != null && tags.size() > 8) {
            errors.add("semanticTags allows at most 8 entries");
        }

        return errors;
    }

    public boolean isValid(EnrichmentResult result) {
        return validate(result).isEmpty();
    }

    private void checkVocab(List<String> errors, String field, List<String> values,
                            Set<String> allowed, int min, int max) {
        if (values == null || values.size() < min || values.size() > max) {
            errors.add(field + " must hold " + min + "–" + max + " values");
            return;
        }
        for (String value : values) {
            if (!allowed.contains(value)) {
                errors.add(field + " has unknown value: " + value);
            }
        }
    }

    private void checkScore(List<String> errors, String field, Integer score) {
        if (score == null || score < 0 || score > 100) {
            errors.add(field + " must be an integer 0–100");
        }
    }
}
