package com.citypulse.catalog.enrichment;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real enrichment client: sends the frozen system prompt plus the per-event
 * user message to the configured chat model (GPT-5.6 Luna via the Spring AI
 * OpenAI starter) and maps the structured JSON reply onto {@link EnrichmentResult}.
 *
 * <p>Wires only when {@code app.enrichment.enabled=true}; a deploy that turns
 * enrichment on must also set {@code spring.ai.model.chat=openai} and
 * {@code OPENAI_API_KEY} so the {@link ChatClient.Builder} is present.
 */
@Component
@ConditionalOnProperty(name = "app.enrichment.enabled", havingValue = "true")
public class SpringAiEnrichmentClient implements EnrichmentClient {

    private final ChatClient chatClient;
    private final EnrichmentPromptFactory prompts;

    public SpringAiEnrichmentClient(ChatClient.Builder chatClientBuilder,
                                    EnrichmentPromptFactory prompts) {
        this.chatClient = chatClientBuilder.build();
        this.prompts = prompts;
    }

    @Override
    public EnrichmentResult enrich(EnrichmentInput input) {
        return chatClient.prompt()
                .system(prompts.systemPrompt())
                .user(prompts.userMessage(input))
                .call()
                .entity(EnrichmentResult.class);
    }
}
