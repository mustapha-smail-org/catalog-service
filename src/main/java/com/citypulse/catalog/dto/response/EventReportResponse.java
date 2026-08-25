package com.citypulse.catalog.dto.response;

import com.citypulse.catalog.dto.request.EventReportType;
import com.citypulse.catalog.entity.EventReportEntity;

import java.time.Instant;

public record EventReportResponse(
        String id,
        String eventId,
        String eventSlug,
        String eventTitle,
        EventReportType type,
        String message,
        String email,
        String status,
        Instant createdAt,
        Instant processedAt,
        String internalNote
) {

    public static EventReportResponse from(EventReportEntity entity) {
        return new EventReportResponse(
                entity.getId().toString(),
                entity.getEventId(),
                entity.getEventSlug(),
                entity.getEventTitle(),
                entity.getType(),
                entity.getMessage(),
                entity.getEmail(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getInternalNote()
        );
    }
}
