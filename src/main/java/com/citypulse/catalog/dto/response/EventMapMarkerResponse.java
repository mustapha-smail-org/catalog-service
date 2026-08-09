package com.citypulse.catalog.dto.response;

import java.time.OffsetDateTime;

public record EventMapMarkerResponse(
        String id,
        String title,
        double latitude,
        double longitude,
        String category,
        String pricing,
        Integer arrondissement,
        OffsetDateTime startAt
) {
}