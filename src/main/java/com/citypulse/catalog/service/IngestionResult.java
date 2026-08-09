package com.citypulse.catalog.service;

import java.time.Instant;

public record IngestionResult(
        String eventId,
        IngestionStatus status,
        Instant sourceUpdatedAt
) {
}