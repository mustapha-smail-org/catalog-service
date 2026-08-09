package com.citypulse.catalog.service;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.EventIdentityConflictException;
import com.citypulse.catalog.mapper.EventEntityUpdater;
import com.citypulse.catalog.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private final EventRepository eventRepository;
    private final EventEntityUpdater eventEntityUpdater;

    @Transactional
    public IngestionResult ingest(EventEntity incoming) {
        Objects.requireNonNull(incoming, "Incoming event must not be null");

        Optional<EventEntity> existingById = eventRepository.findByIdForUpdate(incoming.getId());

        if (existingById.isEmpty()) {
            validateSourceEventIdAvailable(incoming);

            EventEntity inserted = eventRepository.saveAndFlush(incoming);

            log.info("Inserted event id={}", inserted.getId());

            return result(inserted, IngestionStatus.INSERTED);
        }

        EventEntity existing = existingById.orElseThrow();

        reconcileSourceIdentity(existing, incoming);

        IngestionStatus freshness = determineFreshness(existing, incoming);

        if (freshness == IngestionStatus.DUPLICATE) {
            log.info("Ignoring duplicate event id={}, sourceUpdatedAt={}", incoming.getId(), incoming.getSourceUpdatedAt());

            return result(existing, IngestionStatus.DUPLICATE);
        }

        if (freshness == IngestionStatus.STALE) {
            log.info("Ignoring stale event id={}, incoming={}, current={}", incoming.getId(), incoming.getSourceUpdatedAt(), existing.getSourceUpdatedAt());

            return result(existing, IngestionStatus.STALE);
        }

        eventEntityUpdater.update(existing, incoming);

        // Existing is already managed, but flushing here makes database
        // constraint and optimistic-lock failures surface before returning.
        eventRepository.flush();

        log.info("Updated event id={}, sourceUpdatedAt={}", existing.getId(), existing.getSourceUpdatedAt());

        return result(existing, IngestionStatus.UPDATED);
    }

    private void validateSourceEventIdAvailable(EventEntity incoming) {
        Long sourceEventId = incoming.getSourceEventId();

        if (sourceEventId == null) {
            return;
        }

        eventRepository.findBySourceEventIdForUpdate(sourceEventId).ifPresent(conflicting -> {
            throw new EventIdentityConflictException("""
                    Source event ID %d already belongs to event %s \
                    and cannot be assigned to event %s
                    """.formatted(sourceEventId, conflicting.getId(), incoming.getId()));
        });
    }

    private void reconcileSourceIdentity(EventEntity existing, EventEntity incoming) {
        Long currentSourceId = existing.getSourceEventId();
        Long incomingSourceId = incoming.getSourceEventId();

        if (incomingSourceId == null) {
            // Never erase an existing stable source identity.
            return;
        }

        if (currentSourceId != null && !currentSourceId.equals(incomingSourceId)) {
            throw new EventIdentityConflictException("""
                    Event %s already has sourceEventId %d \
                    but received sourceEventId %d
                    """.formatted(existing.getId(), currentSourceId, incomingSourceId));
        }

        if (currentSourceId == null) {
            eventRepository.findBySourceEventIdForUpdate(incomingSourceId).filter(candidate -> !candidate.getId().equals(existing.getId())).ifPresent(conflicting -> {
                throw new EventIdentityConflictException("""
                        Source event ID %d already belongs \
                        to event %s
                        """.formatted(incomingSourceId, conflicting.getId()));
            });

            existing.setSourceEventId(incomingSourceId);
        }
    }

    private IngestionStatus determineFreshness(EventEntity existing, EventEntity incoming) {
        Instant currentTimestamp = existing.getSourceUpdatedAt();
        Instant incomingTimestamp = incoming.getSourceUpdatedAt();

        // Includes the case where both timestamps are null.
        if (Objects.equals(currentTimestamp, incomingTimestamp)) {
            return IngestionStatus.DUPLICATE;
        }

        if (incomingTimestamp == null) {
            return IngestionStatus.STALE;
        }

        if (currentTimestamp == null || incomingTimestamp.isAfter(currentTimestamp)) {
            return IngestionStatus.UPDATED;
        }

        return IngestionStatus.STALE;
    }

    private IngestionResult result(EventEntity event, IngestionStatus status) {
        return new IngestionResult(event.getId(), status, event.getSourceUpdatedAt());
    }
}