package com.citypulse.catalog.enrichment;

import java.util.Set;

/**
 * Closed value sets the enrichment output must draw from (frozen vocab v1).
 * Anything outside these is rejected by {@link EnrichmentValidator}.
 */
public final class EnrichmentVocabulary {

    public static final Set<String> CATEGORIES = Set.of(
            "CONCERT", "CLUBBING", "THEATRE", "DANSE", "SPECTACLE", "CINEMA",
            "EXPOSITION", "FESTIVAL", "ATELIER", "CONFERENCE", "LITTERATURE",
            "GASTRONOMIE", "MARCHE", "SPORT", "BIEN_ETRE", "VISITE",
            "JEUNE_PUBLIC", "NATURE", "AUTRE");

    public static final Set<String> MOODS = Set.of(
            "FESTIF", "ROMANTIQUE", "CHILL", "CULTUREL", "CONVIVIAL",
            "CONTEMPLATIF", "UNDERGROUND", "CHIC", "DECOUVERTE");

    public static final Set<String> SOCIAL_CONTEXTS = Set.of(
            "SOLO", "COUPLE", "ENTRE_AMIS", "EN_FAMILLE", "PROFESSIONNEL");

    public static final Set<String> ENERGY_LEVELS =
            Set.of("CALME", "MODERE", "INTENSE");

    public static final Set<String> ENVIRONMENT_FALLBACKS =
            Set.of("INDOOR", "OUTDOOR");

    private EnrichmentVocabulary() {
    }
}
