package com.citypulse.catalog.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;

public record EventSummaryResponse(
        String id,
        String title,
        String summary,
        Set<String> categories,
        String pricing,
        Integer arrondissement,
        String venue,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String officialUrl
) {
}