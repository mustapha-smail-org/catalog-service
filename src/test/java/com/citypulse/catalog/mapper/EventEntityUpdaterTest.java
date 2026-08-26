package com.citypulse.catalog.mapper;

import com.citypulse.catalog.entity.EventAccessibilityEmbeddable;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnvironment;
import com.citypulse.catalog.entity.EventLocationEmbeddable;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import com.citypulse.catalog.entity.EventPricingEmbeddable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EventEntityUpdaterTest {

    private final EventEntityUpdater updater = new EventEntityUpdater();

    @Test
    void shouldCopyMutableFieldsAndRebuildChildCollections() {
        EventEntity target = new EventEntity("event-1", "Old", Instant.EPOCH);
        String originalSlug = target.getSlug();
        target.replaceCategories(Set.of("Old category"));
        target.addOccurrence(new EventOccurrenceEntity(Instant.EPOCH, null));

        EventEntity source = new EventEntity(
                "event-1", "New title", Instant.parse("2026-08-20T18:00:00Z")
        );
        source.setDescription("New description");
        source.setLeadText("New lead");
        source.setDateDescription("Every Friday");
        source.setUrl("https://citypulse.test/new");
        source.setImageUrl("https://images.test/new.jpg");
        source.setImageAlt("New image alt");
        source.setImageCredit("Photographer");
        source.setTransport("Metro 3");
        source.setEnvironment(EventEnvironment.INDOOR);
        source.setEndDate(Instant.parse("2026-08-20T20:00:00Z"));
        source.setSourceUpdatedAt(Instant.parse("2026-08-13T10:00:00Z"));
        source.setLocation(new EventLocationEmbeddable("Venue", "Street", "75003", "Paris", 1.0, 2.0));
        source.setAccessibility(new EventAccessibilityEmbeddable(true, false, true, "LSF", null));
        source.setPricing(new EventPricingEmbeddable("paid", "10 EUR", "ticket", "url", "Buy"));
        source.replaceCategories(Set.of("Cinema", "Outdoor"));
        source.replaceOccurrences(List.of(
                new EventOccurrenceEntity(Instant.parse("2026-08-21T18:00:00Z"), null),
                new EventOccurrenceEntity(Instant.parse("2026-08-22T18:00:00Z"), Instant.parse("2026-08-22T20:00:00Z"))
        ));

        updater.update(target, source);

        assertThat(target.getTitle()).isEqualTo("New title");
        assertThat(target.getDescription()).isEqualTo("New description");
        assertThat(target.getLeadText()).isEqualTo("New lead");
        assertThat(target.getDateDescription()).isEqualTo("Every Friday");
        assertThat(target.getUrl()).isEqualTo("https://citypulse.test/new");
        assertThat(target.getSlug()).isEqualTo(originalSlug);
        assertThat(target.getImageUrl()).isEqualTo("https://images.test/new.jpg");
        assertThat(target.getImageAlt()).isEqualTo("New image alt");
        assertThat(target.getImageCredit()).isEqualTo("Photographer");
        assertThat(target.getTransport()).isEqualTo("Metro 3");
        assertThat(target.getEnvironment()).isEqualTo(EventEnvironment.INDOOR);
        assertThat(target.getStartDate()).isEqualTo(source.getStartDate());
        assertThat(target.getEndDate()).isEqualTo(source.getEndDate());
        assertThat(target.getLocation()).isSameAs(source.getLocation());
        assertThat(target.getAccessibility()).isSameAs(source.getAccessibility());
        assertThat(target.getPricing()).isSameAs(source.getPricing());
        assertThat(target.getSourceUpdatedAt()).isEqualTo(source.getSourceUpdatedAt());
        assertThat(target.getCategories()).containsExactlyInAnyOrder("Cinema", "Outdoor");
        assertThat(target.getOccurrences()).hasSize(2)
                .allSatisfy(occurrence -> assertThat(occurrence.getEvent()).isSameAs(target));
        assertThat(target.getOccurrences().getFirst()).isNotSameAs(source.getOccurrences().getFirst());
    }
}
