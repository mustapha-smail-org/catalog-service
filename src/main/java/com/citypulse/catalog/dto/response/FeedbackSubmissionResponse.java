package com.citypulse.catalog.dto.response;

import com.citypulse.catalog.dto.request.FeedbackType;
import com.citypulse.catalog.entity.FeedbackSubmissionEntity;

import java.time.Instant;

public record FeedbackSubmissionResponse(
        String id,
        FeedbackType type,
        String message,
        String email,
        String status,
        Instant createdAt,
        Instant processedAt,
        String internalNote
) {

    public static FeedbackSubmissionResponse from(FeedbackSubmissionEntity entity) {
        return new FeedbackSubmissionResponse(
                entity.getId().toString(),
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
