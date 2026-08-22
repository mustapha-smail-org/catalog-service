package com.citypulse.catalog.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public record EventDetailResponse(
        String id,
        String slug,
        String title,
        String description,
        String leadText,
        String dateDescription,
        Set<String> categories,
        String officialUrl,
        String imageUrl,
        String imageAlt,
        String imageCredit,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime displayStartAt,
        OffsetDateTime displayEndAt,
        boolean ongoing,
        OffsetDateTime sourceUpdatedAt,
        String transport,
        Location location,
        Accessibility accessibility,
        Pricing pricing,
        List<Occurrence> occurrences
) {

    public record Location(
            String name,
            String street,
            String zipcode,
            String city,
            Integer arrondissement,
            Double latitude,
            Double longitude
    ) {
    }

    public record Accessibility(
            Boolean wheelchairAccessible,
            Boolean blindAccessible,
            Boolean deafAccessible,
            String signLanguage,
            String mentalAccessibility
    ) {
    }

    public record Pricing(
            String type,
            String detail,
            String accessType,
            String bookingUrl,
            String bookingLinkText
    ) {
    }

    public record Occurrence(
            OffsetDateTime start,
            OffsetDateTime end
    ) {
    }
}
