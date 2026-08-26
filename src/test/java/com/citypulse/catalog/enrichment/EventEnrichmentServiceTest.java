package com.citypulse.catalog.enrichment;

import com.citypulse.catalog.repository.EventRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventEnrichmentServiceTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EnrichmentStore store = mock(EnrichmentStore.class);
    private final EnrichmentClient client = mock(EnrichmentClient.class);
    private final EnrichmentValidator validator = new EnrichmentValidator();

    private final EnrichmentInput input = new EnrichmentInput(
            "Title", null, null, List.of(), null, null, "gratuit", "OUTDOOR");

    private EnrichmentResult validResult() {
        return new EnrichmentResult(
                List.of("CONCERT"), List.of("FESTIF"), List.of("ENTRE_AMIS"),
                List.of("techno"), "INTENSE", null, 60, 70);
    }

    private EventEnrichmentService service(boolean dryRun) {
        EnrichmentProperties props =
                new EnrichmentProperties(true, dryRun, 50, 2, 1, "test-model");
        return new EventEnrichmentService(eventRepository, store, client, validator, props);
    }

    private void selectSingleEvent() {
        when(eventRepository.findIdsNeedingEnrichment(1, 50)).thenReturn(List.of("e1"));
        when(store.loadInput("e1")).thenReturn(Optional.of(input));
    }

    @Test
    void persistsValidEnrichmentViaStore() {
        selectSingleEvent();
        when(client.enrich(any())).thenReturn(validResult());

        EnrichmentBatchReport report = service(false).enrichPending();

        assertThat(report.enriched()).isEqualTo(1);
        assertThat(report.skipped()).isZero();
        verify(store).save(eq("e1"), any(EnrichmentResult.class));
    }

    @Test
    void skipsInvalidEnrichmentWithoutSaving() {
        selectSingleEvent();
        when(client.enrich(any())).thenReturn(new EnrichmentResult(
                List.of("BOGUS"), List.of("FESTIF"), List.of("SOLO"),
                List.of(), "CALME", null, 10, 10));

        EnrichmentBatchReport report = service(false).enrichPending();

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.enriched()).isZero();
        verify(store, never()).save(any(), any());
    }

    @Test
    void retriesTransientFailuresThenGivesUp() {
        selectSingleEvent();
        when(client.enrich(any())).thenThrow(new RuntimeException("boom"));

        EnrichmentBatchReport report = service(false).enrichPending();

        assertThat(report.failed()).isEqualTo(1);
        // maxRetries=2 -> 3 attempts total
        verify(client, times(3)).enrich(any());
        verify(store, never()).save(any(), any());
    }

    @Test
    void dryRunValidatesButDoesNotSave() {
        selectSingleEvent();
        when(client.enrich(any())).thenReturn(validResult());

        EnrichmentBatchReport report = service(true).enrichPending();

        assertThat(report.enriched()).isEqualTo(1);
        verify(store, never()).save(any(), any());
    }
}
