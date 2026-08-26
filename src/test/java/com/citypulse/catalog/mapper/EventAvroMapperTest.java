package com.citypulse.catalog.mapper;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnvironment;
import com.citypulse.events.avro.EventAccessibilityAvro;
import com.citypulse.events.avro.EventAvro;
import com.citypulse.events.avro.EventLocationAvro;
import com.citypulse.events.avro.EventOccurrenceAvro;
import com.citypulse.events.avro.EventPricingAvro;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventAvroMapperTest {

    private final EventAvroMapper mapper = new EventAvroMapper();

    @Test
    void shouldMapCompleteAvroPayload() {
        EventAvro source = completeEvent();

        EventEntity result = mapper.toEntity(source);

        assertThat(result.getId()).isEqualTo("event-42");
        assertThat(result.getSourceEventId()).isEqualTo(42L);
        assertThat(result.getTitle()).isEqualTo("Open-air cinema");
        assertThat(result.getDescription()).isEqualTo("A summer screening");
        assertThat(result.getLeadText()).isEqualTo("A concise introduction");
        assertThat(result.getDateDescription()).isEqualTo("Every evening");
        assertThat(result.getUrl()).isEqualTo("https://citypulse.test/events/42");
        assertThat(result.getSlug()).startsWith("open-air-cinema-");
        assertThat(result.getImageUrl()).isEqualTo("https://images.test/open-air.jpg");
        assertThat(result.getImageAlt()).isEqualTo("People watching a movie outside");
        assertThat(result.getImageCredit()).isEqualTo("City of Paris");
        assertThat(result.getTransport()).isEqualTo("Metro 1");
        assertThat(result.getStartDate()).isEqualTo(Instant.parse("2026-08-20T18:00:00Z"));
        assertThat(result.getEndDate()).isEqualTo(Instant.parse("2026-08-20T21:00:00Z"));
        assertThat(result.getSourceUpdatedAt()).isEqualTo(Instant.parse("2026-08-13T09:00:00Z"));
        assertThat(result.getCategories()).containsExactly("Cinema", "Outdoor");
        assertThat(result.getLocation().getZipcode()).isEqualTo("75001");
        assertThat(result.getLocation().getLatitude()).isEqualTo(48.8566);
        assertThat(result.getAccessibility().getWheelchairAccessible()).isTrue();
        assertThat(result.getAccessibility().getSignLanguage()).isEqualTo("LSF");
        assertThat(result.getPricing().getPriceType()).isEqualTo("gratuit");
        assertThat(result.getPricing().getBookingUrl()).isEqualTo("https://book.test/42");
        assertThat(result.getOccurrences())
                .extracting(occurrence -> occurrence.getStart())
                .containsExactly(Instant.parse("2026-08-20T18:00:00Z"));
        assertThat(result.getOccurrences().getFirst().getEvent()).isSameAs(result);
        assertThat(result.getEnvironment()).isEqualTo(EventEnvironment.OUTDOOR);
    }

    @Test
    void shouldMapOptionalCollectionsWhenTheyAreNull() {
        EventAvro source = completeEvent();
        source.setCategories(null);
        source.setOccurrences(null);

        EventEntity result = mapper.toEntity(source);

        assertThat(result.getCategories()).isEmpty();
        assertThat(result.getOccurrences()).isEmpty();
    }

    @Test
    void shouldFallBackToUnknownForUnrecognisedEnvironment() {
        EventAvro source = completeEvent();
        source.setEnvironment("somewhere-else");

        EventEntity result = mapper.toEntity(source);

        assertThat(result.getEnvironment()).isEqualTo(EventEnvironment.UNKNOWN);
    }

    @Test
    void shouldValidateRequiredPayloadFields() {
        assertThatNullPointerException().isThrownBy(() -> mapper.toEntity(null))
                .withMessage("EventAvro must not be null");

        EventAvro missingId = completeEvent();
        missingId.setId("  ");
        assertThatThrownBy(() -> mapper.toEntity(missingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id must not be blank");

        EventAvro missingTitle = completeEvent();
        missingTitle.setTitle(null);
        assertThatThrownBy(() -> mapper.toEntity(missingTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");

    }

    private EventAvro completeEvent() {
        return EventAvro.newBuilder()
                .setId("event-42")
                .setSourceEventId(42L)
                .setTitle("Open-air cinema")
                .setDescription("A summer screening")
                .setLeadText("A concise introduction")
                .setDateDescription("Every evening")
                .setCategories(List.of("Cinema", "Outdoor"))
                .setUrl("https://citypulse.test/events/42")
                .setImageUrl("https://images.test/open-air.jpg")
                .setImageAlt("People watching a movie outside")
                .setImageCredit("City of Paris")
                .setTransport("Metro 1")
                .setEnvironment("OUTDOOR")
                .setStartDate(Instant.parse("2026-08-20T18:00:00Z"))
                .setEndDate(Instant.parse("2026-08-20T21:00:00Z"))
                .setLocation(EventLocationAvro.newBuilder()
                        .setName("Cour Carrée")
                        .setStreet("Rue de Rivoli")
                        .setZipcode("75001")
                        .setCity("Paris")
                        .setLatitude(48.8566)
                        .setLongitude(2.3522)
                        .build())
                .setOccurrences(List.of(EventOccurrenceAvro.newBuilder()
                        .setStart(Instant.parse("2026-08-20T18:00:00Z"))
                        .setEnd(Instant.parse("2026-08-20T21:00:00Z"))
                        .build()))
                .setAccessibility(EventAccessibilityAvro.newBuilder()
                        .setWheelchairAccessible(true)
                        .setBlindAccessible(false)
                        .setDeafAccessible(true)
                        .setSignLanguage("LSF")
                        .setMentalAccessibility("Quiet area")
                        .build())
                .setPricing(EventPricingAvro.newBuilder()
                        .setPriceType("gratuit")
                        .setPriceDetail("Free entry")
                        .setAccessType("booking")
                        .setBookingUrl("https://book.test/42")
                        .setBookingLinkText("Book")
                        .build())
                .setSourceUpdatedAt(Instant.parse("2026-08-13T09:00:00Z"))
                .build();
    }
}
