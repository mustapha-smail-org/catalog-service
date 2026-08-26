package com.citypulse.catalog.enrichment;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnrichmentEntity;
import com.citypulse.catalog.repository.EventEnrichmentRepository;
import com.citypulse.catalog.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * The enrichment worker: picks events whose enrichment is missing or stale,
 * asks the {@link EnrichmentClient} for a result, validates it, and persists a
 * child {@code event_enrichment} row. Isolated from the Kafka consumer and the
 * REST path (Track B locked decision); only wires when {@code app.enrichment.enabled}.
 *
 * <p>Failure ladder (enrichment is additive — it never blocks serving):
 * transient client errors are retried in-cycle then the event is left for the
 * next cycle; invalid output is held back; both leave the event unenriched.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.enrichment.enabled", havingValue = "true")
public class EventEnrichmentService {

    private final EventRepository eventRepository;
    private final EventEnrichmentRepository enrichmentRepository;
    private final EnrichmentClient client;
    private final EnrichmentValidator validator;
    private final EnrichmentProperties properties;
    private final Clock clock;

    public EventEnrichmentService(EventRepository eventRepository,
                                  EventEnrichmentRepository enrichmentRepository,
                                  EnrichmentClient client,
                                  EnrichmentValidator validator,
                                  EnrichmentProperties properties,
                                  Clock clock) {
        this.eventRepository = eventRepository;
        this.enrichmentRepository = enrichmentRepository;
        this.client = client;
        this.validator = validator;
        this.properties = properties;
        this.clock = clock;
    }

    public EnrichmentBatchReport enrichPending() {
        List<String> ids = eventRepository.findIdsNeedingEnrichment(
                properties.promptVersion(), properties.batchSize());

        int enriched = 0;
        int skipped = 0;
        int failed = 0;

        for (String id : ids) {
            EventEntity event = eventRepository.findById(id).orElse(null);
            if (event == null) {
                continue;
            }

            EnrichmentResult result;
            try {
                result = callWithRetry(event);
            } catch (RuntimeException exception) {
                failed++;
                log.warn("Enrichment failed for {} after retries: {}",
                        id, exception.toString());
                continue;
            }

            List<String> errors = validator.validate(result);
            if (!errors.isEmpty()) {
                skipped++;
                log.warn("Discarding invalid enrichment for {}: {}", id, errors);
                continue;
            }

            if (properties.dryRun()) {
                enriched++;
                log.info("[dry-run] would enrich {}", id);
                continue;
            }

            persist(event, result);
            enriched++;
        }

        EnrichmentBatchReport report =
                new EnrichmentBatchReport(ids.size(), enriched, skipped, failed);
        log.info("Enrichment cycle: {}", report);
        return report;
    }

    private EnrichmentResult callWithRetry(EventEntity event) {
        EnrichmentInput input = toInput(event);
        RuntimeException last = null;

        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            try {
                return client.enrich(input);
            } catch (RuntimeException exception) {
                last = exception;
                log.debug("Enrichment attempt {} for {} failed: {}",
                        attempt, event.getId(), exception.toString());
            }
        }

        throw last;
    }

    private void persist(EventEntity event, EnrichmentResult result) {
        EventEnrichmentEntity entity = enrichmentRepository.findById(event.getId())
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

        // Denormalise onto the event so the RELEVANCE sort avoids a join.
        event.setRankScore(rankScore);
        eventRepository.save(event);
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
