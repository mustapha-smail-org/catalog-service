package com.citypulse.catalog.dto.response;

import java.time.OffsetDateTime;

public record EventMapMarkerResponse(
        String id,
        String slug,
        String title,
        double latitude,
        double longitude,
        String category,
        String pricing,
        Integer arrondissement,
        OffsetDateTime startAt,
        OffsetDateTime displayStartAt,
        OffsetDateTime displayEndAt,
        boolean ongoing,
        String scheduleLabel
) {
}
