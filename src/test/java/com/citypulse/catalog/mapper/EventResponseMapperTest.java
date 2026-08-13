package com.citypulse.catalog.mapper;

import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.entity.EventAccessibilityEmbeddable;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventLocationEmbeddable;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import com.citypulse.catalog.entity.EventPricingEmbeddable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EventResponseMapperTest {

    private final EventResponseMapper mapper = new EventResponseMapper();

    @Test
    void shouldMapSummaryAndTruncateLongDescription() {
        EventEntity event = event();
        event.setDescription("x".repeat(241));

        var result = mapper.toSummary(event);

        assertThat(result.id()).isEqualTo("event-42");
        assertThat(result.summary()).hasSize(240).endsWith("...");
        assertThat(result.categories()).containsExactlyInAnyOrder("Cinema", "Outdoor");
        assertThat(result.pricing()).isEqualTo("FREE");
        assertThat(result.arrondissement()).isEqualTo(1);
        assertThat(result.startAt().getOffset()).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(result.endAt()).isNull();
    }

    @Test
    void shouldMapMarkerUsingAlphabeticallyFirstCategory() {
        var result = mapper.toMapMarker(event());

        assertThat(result.id()).isEqualTo("event-42");
        assertThat(result.category()).isEqualTo("Cinema");
        assertThat(result.latitude()).isEqualTo(48.8566);
        assertThat(result.longitude()).isEqualTo(2.3522);
        assertThat(result.pricing()).isEqualTo("FREE");
    }

    @Test
    void shouldMapCompleteDetailAndSortOccurrences() {
        EventEntity event = event();
        event.addOccurrence(new EventOccurrenceEntity(
                Instant.parse("2026-08-22T18:00:00Z"), null
        ));
        event.addOccurrence(new EventOccurrenceEntity(
                Instant.parse("2026-08-21T18:00:00Z"),
                Instant.parse("2026-08-21T20:00:00Z")
        ));

        EventDetailResponse result = mapper.toDetail(event);

        assertThat(result.location().name()).isEqualTo("Cour Carrée");
        assertThat(result.location().arrondissement()).isEqualTo(1);
        assertThat(result.accessibility().wheelchairAccessible()).isTrue();
        assertThat(result.pricing().type()).isEqualTo("gratuit");
        assertThat(result.occurrences()).extracting(EventDetailResponse.Occurrence::start)
                .containsExactly(
                        Instant.parse("2026-08-21T18:00:00Z").atOffset(ZoneOffset.ofHours(2)),
                        Instant.parse("2026-08-22T18:00:00Z").atOffset(ZoneOffset.ofHours(2))
                );
        assertThat(result.occurrences().getLast().end()).isNull();
    }

    @Test
    void shouldClassifyMissingAndPaidPricing() {
        EventEntity event = event();
        event.getPricing().setPriceType("  ");
        assertThat(mapper.toSummary(event).pricing()).isEqualTo("NOT_SPECIFIED");

        event.getPricing().setPriceType("15 EUR");
        assertThat(mapper.toSummary(event).pricing()).isEqualTo("PAID");
    }

    @Test
    void shouldReturnNoArrondissementForMissingOrNonParisZipcode() {
        EventEntity event = event();
        event.setLocation(new EventLocationEmbeddable(null, null, "69001", "Lyon", 48.0, 2.0));
        assertThat(mapper.toSummary(event).arrondissement()).isNull();

        event.getLocation().setZipcode(null);
        assertThat(mapper.toMapMarker(event).arrondissement()).isNull();
    }

    @Test
    void shouldKeepShortOrMissingDescriptionUnchanged() {
        EventEntity event = event();
        event.setDescription("Short");
        assertThat(mapper.toSummary(event).summary()).isEqualTo("Short");

        event.setDescription(null);
        assertThat(mapper.toSummary(event).summary()).isNull();
    }

    private EventEntity event() {
        EventEntity event = new EventEntity(
                "event-42", "Open-air cinema", Instant.parse("2026-08-20T18:00:00Z")
        );
        event.setDescription("A summer screening");
        event.setUrl("https://citypulse.test/events/42");
        event.setLocation(new EventLocationEmbeddable(
                "Cour Carrée", "Rue de Rivoli", "75001", "Paris", 48.8566, 2.3522
        ));
        event.setAccessibility(new EventAccessibilityEmbeddable(true, false, true, "LSF", null));
        event.setPricing(new EventPricingEmbeddable("gratuit", "Free", "booking", "url", "Book"));
        event.replaceCategories(Set.of("Outdoor", "Cinema"));
        return event;
    }
}
