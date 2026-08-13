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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIngestionServiceEdgeCaseTest {

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
    void shouldRejectNullIncomingEvent() {
        assertThatNullPointerException().isThrownBy(() -> service.ingest(null))
                .withMessage("Incoming event must not be null");
    }

    @Test
    void shouldRejectSourceIdentityAlreadyOwnedByAnotherNewEvent() {
        EventEntity incoming = event("incoming", "2026-08-13T10:00:00Z");
        incoming.setSourceEventId(42L);
        EventEntity owner = event("owner", "2026-08-13T09:00:00Z");
        when(repository.findByIdForUpdate("incoming")).thenReturn(Optional.empty());
        when(repository.findBySourceEventIdForUpdate(42L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.ingest(incoming))
                .isInstanceOf(EventIdentityConflictException.class)
                .hasMessageContaining("owner").hasMessageContaining("incoming");
    }

    @Test
    void shouldAttachPreviouslyUnknownSourceIdentity() {
        EventEntity existing = event("event-1", null);
        EventEntity incoming = event("event-1", "2026-08-13T10:00:00Z");
        incoming.setSourceEventId(42L);
        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));
        when(repository.findBySourceEventIdForUpdate(42L)).thenReturn(Optional.empty());

        IngestionResult result = service.ingest(incoming);

        assertThat(result.status()).isEqualTo(IngestionStatus.UPDATED);
        assertThat(existing.getSourceEventId()).isEqualTo(42L);
        verify(updater).update(existing, incoming);
    }

    @Test
    void shouldKeepExistingIdentityWhenIncomingSourceIdentityIsMissing() {
        EventEntity existing = event("event-1", "2026-08-13T09:00:00Z");
        existing.setSourceEventId(42L);
        EventEntity incoming = event("event-1", "2026-08-13T10:00:00Z");
        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));

        assertThat(service.ingest(incoming).status()).isEqualTo(IngestionStatus.UPDATED);
        assertThat(existing.getSourceEventId()).isEqualTo(42L);
    }

    @Test
    void shouldRejectSourceIdentityOwnedByDifferentExistingEvent() {
        EventEntity existing = event("event-1", "2026-08-13T09:00:00Z");
        EventEntity incoming = event("event-1", "2026-08-13T10:00:00Z");
        incoming.setSourceEventId(42L);
        EventEntity owner = event("owner", "2026-08-13T08:00:00Z");
        when(repository.findByIdForUpdate("event-1")).thenReturn(Optional.of(existing));
        when(repository.findBySourceEventIdForUpdate(42L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.ingest(incoming))
                .isInstanceOf(EventIdentityConflictException.class)
                .hasMessageContaining("owner");
    }

    private EventEntity event(String id, String updatedAt) {
        EventEntity event = new EventEntity(id, "Title", Instant.parse("2026-09-01T18:00:00Z"));
        if (updatedAt != null) {
            event.setSourceUpdatedAt(Instant.parse(updatedAt));
        }
        return event;
    }
}
