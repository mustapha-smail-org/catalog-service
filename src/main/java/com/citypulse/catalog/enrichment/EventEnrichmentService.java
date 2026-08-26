package com.citypulse.catalog.enrichment;

import com.citypulse.catalog.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The enrichment worker: picks events whose enrichment is missing or stale,
 * asks the {@link EnrichmentClient} for a result, validates it, and hands it to
 * {@link EnrichmentStore} to persist. Isolated from the Kafka consumer and the
 * REST path (Track B locked decision); only wires when {@code app.enrichment.enabled}.
 *
 * <p>The model call sits between two short store transactions and is never
 * itself transactional, so no DB connection is held during the (slow) call.
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
    private final EnrichmentStore store;
    private final EnrichmentClient client;
    private final EnrichmentValidator validator;
    private final EnrichmentProperties properties;

    public EventEnrichmentService(EventRepository eventRepository,
                                  EnrichmentStore store,
                                  EnrichmentClient client,
                                  EnrichmentValidator validator,
                                  EnrichmentProperties properties) {
        this.eventRepository = eventRepository;
        this.store = store;
        this.client = client;
        this.validator = validator;
        this.properties = properties;
    }

    public EnrichmentBatchReport enrichPending() {
        List<String> ids = eventRepository.findIdsNeedingEnrichment(
                properties.promptVersion(), properties.batchSize());

        int enriched = 0;
        int skipped = 0;
        int failed = 0;

        for (String id : ids) {
            EnrichmentInput input = store.loadInput(id).orElse(null);
            if (input == null) {
                continue;
            }

            EnrichmentResult result;
            try {
                result = callWithRetry(id, input);
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

            store.save(id, result);
            enriched++;
        }

        EnrichmentBatchReport report =
                new EnrichmentBatchReport(ids.size(), enriched, skipped, failed);
        log.info("Enrichment cycle: {}", report);
        return report;
    }

    private EnrichmentResult callWithRetry(String eventId, EnrichmentInput input) {
        RuntimeException last = null;

        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            try {
                return client.enrich(input);
            } catch (RuntimeException exception) {
                last = exception;
                log.debug("Enrichment attempt {} for {} failed: {}",
                        attempt, eventId, exception.toString());
            }
        }

        throw last;
    }
}
