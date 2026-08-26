package com.citypulse.catalog.enrichment;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentPromptFactoryTest {

    private final EnrichmentPromptFactory factory =
            new EnrichmentPromptFactory(
                    new ClassPathResource("enrichment/system-prompt.txt"));

    @Test
    void systemPromptCarriesTheFrozenContract() {
        String prompt = factory.systemPrompt();
        assertThat(prompt).contains("categories", "moodAffinities", "energyLevel");
        assertThat(prompt).contains("environmentFallback");
        assertThat(prompt).doesNotContain("MODE_DESIGN");
    }

    @Test
    void userMessageIncludesPresentFieldsAndOmitsEmptyOnes() {
        EnrichmentInput input = new EnrichmentInput(
                "Nuit techno", "Sur les toits", "Une soirée sur un rooftop",
                List.of("Concert", "Nuit"), "Le Rooftop", 11, "payant", "OUTDOOR");

        String message = factory.userMessage(input);

        assertThat(message).contains("Titre : Nuit techno");
        assertThat(message).contains("Tags source : Concert;Nuit");
        assertThat(message).contains("Arrondissement : 11");
        assertThat(message).contains("environment : OUTDOOR");
    }

    @Test
    void userMessageOmitsBlankAndNullFields() {
        EnrichmentInput input = new EnrichmentInput(
                "Titre seul", null, "  ", List.of(), null, null, null, "UNKNOWN");

        String message = factory.userMessage(input);

        assertThat(message).contains("Titre : Titre seul");
        assertThat(message).doesNotContain("Accroche");
        assertThat(message).doesNotContain("Description");
        assertThat(message).doesNotContain("Tags source");
        assertThat(message).doesNotContain("Arrondissement");
        assertThat(message).contains("environment : UNKNOWN");
    }
}
