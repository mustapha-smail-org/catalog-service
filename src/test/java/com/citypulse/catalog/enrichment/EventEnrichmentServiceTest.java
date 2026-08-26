package com.citypulse.catalog.enrichment;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnrichmentEntity;
import com.citypulse.catalog.repository.EventEnrichmentRepository;
import com.citypulse.catalog.repository.EventRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventEnrichmentServiceTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventEnrichmentRepository enrichmentRepository =
            mock(EventEnrichmentRepository.class);
    private final EnrichmentClient client = mock(EnrichmentClient.class);
    private final EnrichmentValidator validator = new EnrichmentValidator();
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-08-26T10:00:00Z"), ZoneOffset.UTC);

    private EnrichmentResult validResult() {
        return new EnrichmentResult(
                List.of("CONCERT"), List.of("FESTIF"), List.of("ENTRE_AMIS"),
                List.of("techno"), "INTENSE", null, 60, 70);
    }

    private EventEnrichmentService service(boolean dryRun) {
        EnrichmentProperties props =
                new EnrichmentProperties(true, dryRun, 50, 2, 1, "test-model");
        return new EventEnrichmentService(
                eventRepository, enrichmentRepository, client, validator, props, clock);
    }

    private void selectSingleEvent() {
        EventEntity event =
                new EventEntity("e1", "Title", Instant.parse("2026-08-20T18:00:00Z"));
        when(eventRepository.findIdsNeedingEnrichment(1, 50)).thenReturn(List.of("e1"));
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));
        when(enrichmentRepository.findById("e1")).thenReturn(Optional.empty());
    }

    @Test
    void persistsValidEnrichment() {
        selectSingleEvent();
        when(client.enrich(any())).thenReturn(validResult());

        EnrichmentBatchReport report = service(false).enrichPending();

        assertThat(report.enriched()).isEqualTo(1);
        assertThat(report.skipped()).isZero();
        verify(enrichmentRepository).save(any(EventEnrichmentEntity.class));
    }

    @Test
    void skipsInvalidEnrichmentWithoutPersisting() {
        selectSingleEvent();
        when(client.enrich(any())).thenReturn(new EnrichmentResult(
                List.of("BOGUS"), List.of("FESTIF"), List.of("SOLO"),
                List.of(), "CALME", null, 10, 10));

        EnrichmentBatchReport report = service(false).enrichPending();

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.enriched()).isZero();
        verify(enrichmentRepository, never()).save(any());
    }

    @Test
    void retriesTransientFailuresThenGivesUp() {
        selectSingleEvent();
        when(client.enrich(any())).thenThrow(new RuntimeException("boom"));

        EnrichmentBatchReport report = service(false).enrichPending();

        assertThat(report.failed()).isEqualTo(1);
        // maxRetries=2 -> 3 attempts total
        verify(client, times(3)).enrich(any());
        verify(enrichmentRepository, never()).save(any());
    }

    @Test
    void dryRunValidatesButDoesNotPersist() {
        selectSingleEvent();
        when(client.enrich(any())).thenReturn(validResult());

        EnrichmentBatchReport report = service(true).enrichPending();

        assertThat(report.enriched()).isEqualTo(1);
        verify(enrichmentRepository, never()).save(any());
    }
}
