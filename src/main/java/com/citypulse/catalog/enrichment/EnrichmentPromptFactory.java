package com.citypulse.catalog.enrichment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Builds the two prompt halves for the enrichment call: the frozen system
 * prompt (loaded once from a classpath resource) and the per-event user message
 * (empty fields omitted so absence reads as absence). Kept separate from the
 * client so the prompt text is unit-testable without any model wiring.
 */
@Component
public class EnrichmentPromptFactory {

    private final String systemPrompt;

    public EnrichmentPromptFactory(
            @Value("classpath:enrichment/system-prompt.txt") Resource resource) {
        try {
            this.systemPrompt = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to load enrichment system prompt", exception);
        }
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String userMessage(EnrichmentInput input) {
        StringBuilder builder = new StringBuilder("Événement à enrichir :\n\n");
        append(builder, "Titre", input.title());
        append(builder, "Accroche", input.leadText());
        append(builder, "Description", input.description());
        if (input.rawCategories() != null && !input.rawCategories().isEmpty()) {
            append(builder, "Tags source", String.join(";", input.rawCategories()));
        }
        append(builder, "Lieu", input.venue());
        if (input.arrondissement() != null) {
            append(builder, "Arrondissement", input.arrondissement().toString());
        }
        append(builder, "Tarif", input.priceType());
        append(builder, "environment", input.environment());
        return builder.toString();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(" : ").append(value.strip()).append('\n');
        }
    }
}
