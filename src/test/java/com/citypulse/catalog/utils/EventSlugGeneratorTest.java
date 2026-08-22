package com.citypulse.catalog.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventSlugGeneratorTest {

    @Test
    void transliteratesFrenchAccentsAndLigatures() {
        assertThat(EventSlugGenerator.generate("Cœur, été & œuvre", "stable-id"))
                .startsWith("coeur-ete-oeuvre-")
                .matches("coeur-ete-oeuvre-[a-f0-9]{8}");
    }
}
