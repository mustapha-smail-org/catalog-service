package com.citypulse.catalog.mapper;

import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EventResponseMapper {

    private static final ZoneId PARIS =
            ZoneId.of("Europe/Paris");

    private final Clock clock;

    public EventSummaryResponse toSummary(EventEntity event) {
        Schedule schedule = schedule(event);
        return new EventSummaryResponse(
                event.getId(),
                event.getSlug(),
                event.getTitle(),
                summarize(firstText(event.getLeadText(), event.getDescription())),
                categories(event),
                pricingCategory(event),
                arrondissement(event),
                event.getLocation().getName(),
                parisTime(event.getStartDate()),
                parisTime(event.getEndDate()),
                parisTime(schedule.start()),
                parisTime(schedule.end()),
                schedule.ongoing(),
                plainText(event.getDateDescription()),
                event.getUrl(),
                event.getImageUrl(),
                event.getImageAlt(),
                event.getImageCredit(),
                parisTime(event.getSourceUpdatedAt()),
                environmentName(event),
                enrichment(event)
        );
    }

    public EventMapMarkerResponse toMapMarker(EventEntity event) {
        Schedule schedule = schedule(event);
        return new EventMapMarkerResponse(
                event.getId(),
                event.getSlug(),
                event.getTitle(),
                event.getLocation().getLatitude(),
                event.getLocation().getLongitude(),
                event.getCategories().stream()
                        .sorted()
                        .findFirst()
                        .orElse(null),
                pricingCategory(event),
                arrondissement(event),
                parisTime(event.getStartDate()),
                parisTime(schedule.start()),
                parisTime(schedule.end()),
                schedule.ongoing(),
                plainText(event.getDateDescription())
        );
    }

    public EventDetailResponse toDetail(EventEntity event) {
        Schedule schedule = schedule(event);
        return new EventDetailResponse(
                event.getId(),
                event.getSlug(),
                event.getTitle(),
                event.getDescription(),
                event.getLeadText(),
                event.getDateDescription(),
                categories(event),
                event.getUrl(),
                event.getImageUrl(),
                event.getImageAlt(),
                event.getImageCredit(),
                parisTime(event.getStartDate()),
                parisTime(event.getEndDate()),
                parisTime(schedule.start()),
                parisTime(schedule.end()),
                schedule.ongoing(),
                parisTime(event.getSourceUpdatedAt()),
                event.getTransport(),
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
                        .toList(),
                environmentName(event),
                enrichment(event)
        );
    }

    private String summarize(String value) {
        String text = plainText(value);
        if (text == null || text.length() <= 240) {
            return text;
        }

        int boundary = text.lastIndexOf(' ', 237);
        int end = boundary < 180 ? 237 : boundary;
        return text.substring(0, end).stripTrailing() + "...";
    }

    private String plainText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String withoutUnsafeBlocks = value
                .replaceAll("(?is)<(script|style|iframe|object|template)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</li>|</h[1-6]>", " ")
                .replaceAll("<[^>]+>", " ");

        String text = HtmlUtils.htmlUnescape(withoutUnsafeBlocks)
                .replaceAll("\\s+", " ")
                .trim();
        return text.isBlank() ? null : text;
    }

    private String firstText(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private Schedule schedule(EventEntity event) {
        Instant now = clock.instant();
        EventOccurrenceEntity next = event.getOccurrences().stream()
                .filter(occurrence -> occurrence.getEnd() == null
                        ? !occurrence.getStart().isBefore(now)
                        : !occurrence.getEnd().isBefore(now))
                .min(Comparator.comparing(EventOccurrenceEntity::getStart))
                .orElse(null);

        if (next != null) {
            return new Schedule(next.getStart(), next.getEnd(), false);
        }

        boolean ongoing = event.getEndDate() != null
                && !event.getStartDate().isAfter(now)
                && !event.getEndDate().isBefore(now);
        if (ongoing) {
            return new Schedule(null, event.getEndDate(), true);
        }

        return new Schedule(event.getStartDate(), event.getEndDate(), false);
    }

    private record Schedule(Instant start, Instant end, boolean ongoing) {
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
            case "gratuit sous condition" -> "FREE_CONDITIONAL";
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

    private com.citypulse.catalog.dto.response.EventEnrichmentResponse
            enrichment(EventEntity event) {
        var source = event.getEnrichment();
        if (source == null) {
            return null;
        }
        return new com.citypulse.catalog.dto.response.EventEnrichmentResponse(
                java.util.List.copyOf(source.getNormCategories()),
                java.util.List.copyOf(source.getMoodAffinities()),
                java.util.List.copyOf(source.getSocialContexts()),
                java.util.List.copyOf(source.getSemanticTags()),
                source.getEnergyLevel(),
                source.getEnvironmentFallback(),
                source.getUniquenessScore(),
                source.getQualityScore(),
                source.getRankScore()
        );
    }

    private String environmentName(EventEntity event) {
        var environment = event.getEnvironment();
        return (environment == null
                ? com.citypulse.catalog.entity.EventEnvironment.UNKNOWN
                : environment).name();
    }

    private OffsetDateTime parisTime(Instant value) {
        return value == null
                ? null
                : value.atZone(PARIS).toOffsetDateTime();
    }
}
