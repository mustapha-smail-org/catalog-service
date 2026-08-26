package com.citypulse.catalog.mapper;

import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.entity.EventAccessibilityEmbeddable;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnrichmentEntity;
import com.citypulse.catalog.entity.EventEnvironment;
import com.citypulse.catalog.entity.EventLocationEmbeddable;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import com.citypulse.catalog.entity.EventPricingEmbeddable;

import java.util.List;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EventResponseMapperTest {

    private final EventResponseMapper mapper = new EventResponseMapper(
            Clock.fixed(Instant.parse("2026-08-20T10:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void shouldMapSummaryAndTruncateLongDescription() {
        EventEntity event = event();
        event.setDescription("x".repeat(241));

        var result = mapper.toSummary(event);

        assertThat(result.id()).isEqualTo("event-42");
        assertThat(result.slug()).startsWith("open-air-cinema-");
        assertThat(result.summary()).hasSize(240).endsWith("...");
        assertThat(result.categories()).containsExactlyInAnyOrder("Cinema", "Outdoor");
        assertThat(result.pricing()).isEqualTo("FREE");
        assertThat(result.arrondissement()).isEqualTo(1);
        assertThat(result.imageUrl()).isEqualTo("https://images.test/open-air.jpg");
        assertThat(result.imageAlt()).isEqualTo("People watching a movie outside");
        assertThat(result.startAt().getOffset()).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(result.endAt()).isNull();
        assertThat(result.displayStartAt()).isEqualTo(result.startAt());
        assertThat(result.ongoing()).isFalse();
        assertThat(result.environment()).isEqualTo("OUTDOOR");
        assertThat(result.enrichment()).isNull();
    }

    @Test
    void usesEnrichmentEnvironmentFallbackWhenApiValueUnknown() {
        EventEntity event = event();
        event.setEnvironment(EventEnvironment.UNKNOWN);
        EventEnrichmentEntity enrichment = new EventEnrichmentEntity(event);
        enrichment.setEnvironmentFallback("INDOOR");
        event.setEnrichment(enrichment);

        assertThat(mapper.toSummary(event).environment()).isEqualTo("INDOOR");

        // A known API value wins over the AI fallback.
        event.setEnvironment(EventEnvironment.OUTDOOR);
        assertThat(mapper.toSummary(event).environment()).isEqualTo("OUTDOOR");
    }

    @Test
    void shouldMapEnrichmentBlockWhenPresent() {
        EventEntity event = event();
        EventEnrichmentEntity enrichment = new EventEnrichmentEntity(event);
        enrichment.setNormCategories(List.of("CINEMA"));
        enrichment.setMoodAffinities(List.of("CHILL", "DECOUVERTE"));
        enrichment.setSocialContexts(List.of("COUPLE"));
        enrichment.setSemanticTags(List.of("plein air", "nocturne"));
        enrichment.setEnergyLevel("CALME");
        enrichment.setUniquenessScore(58);
        enrichment.setQualityScore(71);
        enrichment.setRankScore(0.42);
        event.setEnrichment(enrichment);

        var summary = mapper.toSummary(event).enrichment();
        assertThat(summary).isNotNull();
        assertThat(summary.categories()).containsExactly("CINEMA");
        assertThat(summary.moodAffinities()).containsExactly("CHILL", "DECOUVERTE");
        assertThat(summary.socialContexts()).containsExactly("COUPLE");
        assertThat(summary.energyLevel()).isEqualTo("CALME");
        assertThat(summary.uniquenessScore()).isEqualTo(58);
        assertThat(summary.qualityScore()).isEqualTo(71);
        assertThat(summary.rankScore()).isEqualTo(0.42);

        assertThat(mapper.toDetail(event).enrichment().semanticTags())
                .containsExactly("plein air", "nocturne");
    }

    @Test
    void shouldMapMarkerUsingAlphabeticallyFirstCategory() {
        var result = mapper.toMapMarker(event());

        assertThat(result.id()).isEqualTo("event-42");
        assertThat(result.slug()).startsWith("open-air-cinema-");
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

        assertThat(result.slug()).startsWith("open-air-cinema-");
        assertThat(result.imageUrl()).isEqualTo("https://images.test/open-air.jpg");
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
        assertThat(result.environment()).isEqualTo("OUTDOOR");
    }

    @Test
    void shouldClassifyMissingAndPaidPricing() {
        EventEntity event = event();
        event.getPricing().setPriceType("  ");
        assertThat(mapper.toSummary(event).pricing()).isEqualTo("NOT_SPECIFIED");

        event.getPricing().setPriceType("15 EUR");
        assertThat(mapper.toSummary(event).pricing()).isEqualTo("PAID");

        event.getPricing().setPriceType("gratuit sous condition");
        assertThat(mapper.toSummary(event).pricing()).isEqualTo("FREE_CONDITIONAL");
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

    @Test
    void shouldStripMarkupAndDecodeEntitiesInSummary() {
        EventEntity event = event();
        event.setLeadText("<p>Un <strong>concert</strong> &amp; une expo.</p>");

        assertThat(mapper.toSummary(event).summary())
                .isEqualTo("Un concert & une expo.");
    }

    @Test
    void shouldExposeOngoingScheduleWithoutHistoricalDisplayStart() {
        EventEntity event = event();
        event.setStartDate(Instant.parse("2023-10-23T00:00:00Z"));
        event.setEndDate(Instant.parse("2026-08-31T00:00:00Z"));
        event.setDateDescription("<p>Ouvert tous les jours</p>");

        var result = mapper.toSummary(event);

        assertThat(result.ongoing()).isTrue();
        assertThat(result.displayStartAt()).isNull();
        assertThat(result.displayEndAt()).isEqualTo(
                Instant.parse("2026-08-31T00:00:00Z").atOffset(ZoneOffset.ofHours(2))
        );
        assertThat(result.scheduleLabel()).isEqualTo("Ouvert tous les jours");
    }

    private EventEntity event() {
        EventEntity event = new EventEntity(
                "event-42", "Open-air cinema", Instant.parse("2026-08-20T18:00:00Z")
        );
        event.setDescription("A summer screening");
        event.setUrl("https://citypulse.test/events/42");
        event.setImageUrl("https://images.test/open-air.jpg");
        event.setImageAlt("People watching a movie outside");
        event.setLocation(new EventLocationEmbeddable(
                "Cour Carrée", "Rue de Rivoli", "75001", "Paris", 48.8566, 2.3522
        ));
        event.setAccessibility(new EventAccessibilityEmbeddable(true, false, true, "LSF", null));
        event.setPricing(new EventPricingEmbeddable("gratuit", "Free", "booking", "url", "Book"));
        event.replaceCategories(Set.of("Outdoor", "Cinema"));
        event.setEnvironment(EventEnvironment.OUTDOOR);
        return event;
    }
}
