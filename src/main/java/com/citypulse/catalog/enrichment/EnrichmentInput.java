package com.citypulse.catalog.enrichment;

import java.util.List;

/**
 * Event fields fed to the enrichment client (the prompt inputs). Built from an
 * {@code EventEntity}; nothing here is fetched anew.
 */
public record EnrichmentInput(
        String title,
        String leadText,
        String description,
        List<String> rawCategories,
        String venue,
        Integer arrondissement,
        String priceType,
        String environment
) {
}
