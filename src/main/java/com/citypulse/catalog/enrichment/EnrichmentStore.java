package com.citypulse.catalog.enrichment;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnrichmentEntity;
import com.citypulse.catalog.repository.EventEnrichmentRepository;
import com.citypulse.catalog.repository.EventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * All database access for enrichment, kept in short transactions on a separate
 * bean so the (slow, external) model call in {@link EventEnrichmentService}
 * never runs inside a transaction or holds a connection. Reads happen with the
 * session open, so LAZY collections (categories) initialise safely. Registered
 * unconditionally (it is dormant unless the enabled worker calls it), which
 * keeps it directly testable without wiring the model client.
 */
@Component
public class EnrichmentStore {

    private final EventRepository eventRepository;
    private final EventEnrichmentRepository enrichmentRepository;
    private final EnrichmentProperties properties;
    private final Clock clock;

    public EnrichmentStore(EventRepository eventRepository,
                           EventEnrichmentRepository enrichmentRepository,
                           EnrichmentProperties properties,
                           Clock clock) {
        this.eventRepository = eventRepository;
        this.enrichmentRepository = enrichmentRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<EnrichmentInput> loadInput(String eventId) {
        return eventRepository.findById(eventId).map(this::toInput);
    }

    /**
     * Upsert the enrichment row and denormalise its rank onto the event, both in
     * one transaction. The event is loaded managed here, so setting its
     * rank_score is dirty-checked (no detached-merge, no lazy access).
     */
    @Transactional
    public void save(String eventId, EnrichmentResult result) {
        EventEntity event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }

        EventEnrichmentEntity entity = enrichmentRepository.findById(eventId)
                .orElseGet(() -> new EventEnrichmentEntity(event));

        entity.setNormCategories(result.categories());
        entity.setMoodAffinities(result.moodAffinities());
        entity.setSocialContexts(result.socialContexts());
        entity.setSemanticTags(result.semanticTags() == null
                ? List.of() : result.semanticTags());
        entity.setEnergyLevel(result.energyLevel());
        entity.setEnvironmentFallback(result.environmentFallback());
        entity.setUniquenessScore(result.uniquenessScore());
        entity.setQualityScore(result.qualityScore());
        double rankScore = EnrichmentRankScorer.score(
                result.uniquenessScore(), result.qualityScore());
        entity.setRankScore(rankScore);
        entity.setEnrichmentModel(properties.model());
        entity.setEnrichmentVersion(properties.promptVersion());
        entity.setEnrichmentSourceVersion(event.getSourceUpdatedAt());
        entity.setEnrichedAt(clock.instant());
        enrichmentRepository.save(entity);

        // Denormalise onto the event (managed → flushed) for the RELEVANCE sort.
        event.setRankScore(rankScore);
    }

    private EnrichmentInput toInput(EventEntity event) {
        return new EnrichmentInput(
                event.getTitle(),
                event.getLeadText(),
                event.getDescription(),
                List.copyOf(event.getCategories()),
                event.getLocation() == null ? null : event.getLocation().getName(),
                arrondissement(event),
                event.getPricing() == null ? null : event.getPricing().getPriceType(),
                event.getEnvironment() == null ? null : event.getEnvironment().name());
    }

    private Integer arrondissement(EventEntity event) {
        if (event.getLocation() == null) {
            return null;
        }
        String zipcode = event.getLocation().getZipcode();
        if (zipcode == null || !zipcode.matches("750(?:0[1-9]|1[0-9]|20)")) {
            return null;
        }
        return Integer.parseInt(zipcode.substring(3));
    }
}
