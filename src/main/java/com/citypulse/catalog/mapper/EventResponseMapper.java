package com.citypulse.catalog.mapper;

import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class EventResponseMapper {

    private static final ZoneId PARIS =
            ZoneId.of("Europe/Paris");

    public EventSummaryResponse toSummary(EventEntity event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                summarize(event.getDescription()),
                categories(event),
                pricingCategory(event),
                arrondissement(event),
                event.getLocation().getName(),
                parisTime(event.getStartDate()),
                parisTime(event.getEndDate()),
                event.getUrl()
        );
    }

    public EventMapMarkerResponse toMapMarker(EventEntity event) {
        return new EventMapMarkerResponse(
                event.getId(),
                event.getTitle(),
                event.getLocation().getLatitude(),
                event.getLocation().getLongitude(),
                event.getCategories().stream()
                        .sorted()
                        .findFirst()
                        .orElse(null),
                pricingCategory(event),
                arrondissement(event),
                parisTime(event.getStartDate())
        );
    }

    public EventDetailResponse toDetail(EventEntity event) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                categories(event),
                event.getUrl(),
                parisTime(event.getStartDate()),
                parisTime(event.getEndDate()),
                new EventDetailResponse.Location(
                        event.getLocation().getName(),
                        event.getLocation().getStreet(),
                        event.getLocation().getZipcode(),
                        event.getLocation().getCity(),
                        arrondissement(event),
                        event.getLocation().getLatitude(),
                        event.getLocation().getLongitude()
                ),
                new EventDetailResponse.Accessibility(
                        event.getAccessibility()
                                .getWheelchairAccessible(),
                        event.getAccessibility().getBlindAccessible(),
                        event.getAccessibility().getDeafAccessible(),
                        event.getAccessibility().getSignLanguage(),
                        event.getAccessibility()
                                .getMentalAccessibility()
                ),
                new EventDetailResponse.Pricing(
                        event.getPricing().getPriceType(),
                        event.getPricing().getPriceDetail(),
                        event.getPricing().getAccessType(),
                        event.getPricing().getBookingUrl(),
                        event.getPricing().getBookingLinkText()
                ),
                event.getOccurrences().stream()
                        .sorted(Comparator.comparing(
                                EventOccurrenceEntity::getStart
                        ))
                        .map(occurrence ->
                                new EventDetailResponse.Occurrence(
                                        parisTime(occurrence.getStart()),
                                        parisTime(occurrence.getEnd())
                                )
                        )
                        .toList()
        );
    }

    private String summarize(String description) {
        if (description == null || description.length() <= 240) {
            return description;
        }

        return description.substring(0, 237) + "...";
    }

    private Set<String> categories(EventEntity event) {
        return new LinkedHashSet<>(event.getCategories());
    }

    private String pricingCategory(EventEntity event) {
        String value = event.getPricing().getPriceType();

        if (value == null || value.isBlank()) {
            return "NOT_SPECIFIED";
        }

        return switch (value.trim().toLowerCase()) {
            case "free", "gratuit", "gratuite" -> "FREE";
            default -> "PAID";
        };
    }

    private Integer arrondissement(EventEntity event) {
        if (event == null) {
            return null;
        }
        var location = event.getLocation();
        if (location == null) {
            return null;
        }

        String zipcode = location.getZipcode();
        if (zipcode == null || !zipcode.matches("750(?:0[1-9]|1[0-9]|20)")) {
            return null;
        }

        return Integer.parseInt(zipcode.substring(3));
    }

    private OffsetDateTime parisTime(Instant value) {
        return value == null
                ? null
                : value.atZone(PARIS).toOffsetDateTime();
    }
}
