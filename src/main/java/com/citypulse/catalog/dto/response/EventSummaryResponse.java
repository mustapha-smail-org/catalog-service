package com.citypulse.catalog.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;

public record EventSummaryResponse(
        String id,
        String slug,
        String title,
        String summary,
        Set<String> categories,
        String pricing,
        Integer arrondissement,
        String venue,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime displayStartAt,
        OffsetDateTime displayEndAt,
        boolean ongoing,
        String scheduleLabel,
        String officialUrl,
        String imageUrl,
        String imageAlt,
        String imageCredit,
        OffsetDateTime sourceUpdatedAt,
        String environment,
        EventEnrichmentResponse enrichment
) {
}
