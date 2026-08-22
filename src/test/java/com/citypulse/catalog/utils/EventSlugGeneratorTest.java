package com.citypulse.catalog.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventSlugGeneratorTest {

    @Test
    void transliteratesFrenchAccentsAndLigatures() {
        assertThat(EventSlugGenerator.generate("Cœur, été & œuvre", "stable-id"))
                .isEqualTo("coeur-ete-oeuvre-b1def59c");
    }

    @Test
    void collapsesSeparatorsAndUsesFallbacksForMissingValues() {
        assertThat(EventSlugGenerator.generate("  Open---Air !!! Cinema  ", null))
                .matches("open-air-cinema-[a-f0-9]{8}");
        assertThat(EventSlugGenerator.generate(null, "stable-id"))
                .matches("event-[a-f0-9]{8}");
    }

    @Test
    void truncatesLongTitlesWithoutLeavingATrailingSeparator() {
        String longTitle = "a".repeat(179) + " - " + "b".repeat(50);

        assertThat(EventSlugGenerator.generate(longTitle, "stable-id"))
                .matches("a{179}-[a-f0-9]{8}")
                .hasSize(188);
    }
}
