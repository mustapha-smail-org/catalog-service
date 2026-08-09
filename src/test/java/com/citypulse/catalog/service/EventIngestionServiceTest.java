package com.citypulse.catalog.service;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.EventIdentityConflictException;
import com.citypulse.catalog.mapper.EventEntityUpdater;
import com.citypulse.catalog.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventIngestionServiceTest {

    @Mock
    private EventRepository repository;

    @Mock
    private EventEntityUpdater updater;

    private EventIngestionService service;

    @BeforeEach
    void setUp() {
        service = new EventIngestionService(repository, updater);
    }

    @Test
    void shouldInsertUnknownEvent() {
        EventEntity incoming = event("event-1", "2026-08-09T10:00:00Z");

        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.empty());

        when(repository.saveAndFlush(incoming)).thenReturn(incoming);

        IngestionResult result = service.ingest(incoming);

        assertThat(result.status()).isEqualTo(IngestionStatus.INSERTED);

        verify(repository).saveAndFlush(incoming);
        verifyNoInteractions(updater);
    }

    @Test
    void shouldUpdateWhenIncomingEventIsNewer() {
        EventEntity existing = event("event-1", "2026-08-09T10:00:00Z");

        EventEntity incoming = event("event-1", "2026-08-09T11:00:00Z");

        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));

        IngestionResult result = service.ingest(incoming);

        assertThat(result.status()).isEqualTo(IngestionStatus.UPDATED);

        verify(updater).update(existing, incoming);
        verify(repository).flush();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldIgnoreDuplicateDelivery() {
        EventEntity existing = event("event-1", "2026-08-09T10:00:00Z");

        EventEntity incoming = event("event-1", "2026-08-09T10:00:00Z");

        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));

        IngestionResult result = service.ingest(incoming);

        assertThat(result.status()).isEqualTo(IngestionStatus.DUPLICATE);

        verifyNoInteractions(updater);
        verify(repository, never()).flush();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldIgnoreOlderDelivery() {
        EventEntity existing = event("event-1", "2026-08-09T11:00:00Z");

        EventEntity incoming = event("event-1", "2026-08-09T10:00:00Z");

        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));

        IngestionResult result = service.ingest(incoming);

        assertThat(result.status()).isEqualTo(IngestionStatus.STALE);

        verifyNoInteractions(updater);
        verify(repository, never()).flush();
    }

    @Test
    void shouldRejectConflictingSourceIdentity() {
        EventEntity existing = event("event-1", "2026-08-09T10:00:00Z");
        existing.setSourceEventId(100L);

        EventEntity incoming = event("event-1", "2026-08-09T11:00:00Z");
        incoming.setSourceEventId(200L);

        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.ingest(incoming)).isInstanceOf(EventIdentityConflictException.class).hasMessageContaining("event-1").hasMessageContaining("100").hasMessageContaining("200");

        verifyNoInteractions(updater);
    }

    private EventEntity event(String id, String sourceUpdatedAt) {
        EventEntity event = new EventEntity(id, "Test event", Instant.parse("2026-09-01T18:00:00Z"));

        event.setSourceUpdatedAt(Instant.parse(sourceUpdatedAt));

        return event;
    }
}