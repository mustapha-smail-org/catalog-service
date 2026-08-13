package com.citypulse.catalog.repository;

import com.citypulse.catalog.entity.*;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("citypulse_catalog")
            .withUsername("citypulse")
            .withPassword("citypulse");
    @Autowired
    private EventRepository repository;
    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldPersistAndReloadCompleteEvent() {
        Instant start = Instant.parse("2026-08-10T16:00:00Z");

        Instant end = Instant.parse("2026-08-10T18:00:00Z");

        EventEntity event = getEventEntity(start, end);

        event.replaceCategories(Set.of("Cinema", "Outdoor"));

        event.addOccurrence(new EventOccurrenceEntity(start, end));

        repository.saveAndFlush(event);

        entityManager.clear();

        EventEntity result = repository.findById("event-123").orElseThrow();

        assertThat(result.getTitle()).isEqualTo("Outdoor cinema");

        assertThat(result.getSourceEventId()).isEqualTo(123L);

        assertThat(result.getLocation().getCity()).isEqualTo("Paris");

        assertThat(result.getAccessibility().getWheelchairAccessible()).isTrue();

        assertThat(result.getPricing().getPriceType()).isEqualTo("gratuit");

        assertThat(result.getCategories()).containsExactlyInAnyOrder("Cinema", "Outdoor");

        assertThat(result.getOccurrences()).hasSize(1);

        assertThat(result.getOccurrences().getFirst().getStart()).isEqualTo(start);

        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    private static @NonNull EventEntity getEventEntity(Instant start, Instant end) {
        EventEntity event = new EventEntity("event-123", "Outdoor cinema", start);

        event.setSourceEventId(123L);
        event.setDescription("Cinema in Paris");
        event.setUrl("https://example.com/events/123");
        event.setEndDate(end);

        event.setLocation(new EventLocationEmbeddable("Parc de Paris", "1 rue de Paris", "75012", "Paris", 48.8566, 2.3522));

        event.setAccessibility(new EventAccessibilityEmbeddable(true, false, true, "French sign language", "false"));

        event.setPricing(new EventPricingEmbeddable("gratuit", "Free entry", "libre", "https://example.com/booking", "Book"));
        return event;
    }

    @Test
    void shouldFindEventBySourceEventId() {
        EventEntity event = new EventEntity("event-456", "Paris exhibition", Instant.parse("2026-09-01T08:00:00Z"));

        event.setSourceEventId(456L);

        repository.saveAndFlush(event);

        assertThat(repository.findBySourceEventId(456L)).isPresent().get().extracting(EventEntity::getId).isEqualTo("event-456");
    }

    @Test
    void shouldReturnDistinctCategories() {
        EventEntity first = new EventEntity("event-1", "First event", Instant.parse("2026-09-01T08:00:00Z"));

        first.replaceCategories(Set.of("Cinema", "Outdoor"));

        EventEntity second = new EventEntity("event-2", "Second event", Instant.parse("2026-09-02T08:00:00Z"));

        second.replaceCategories(Set.of("Cinema", "Music"));

        repository.saveAllAndFlush(List.of(first, second));

        assertThat(repository.findDistinctCategories()).containsExactly("Cinema", "Music", "Outdoor");
    }

    @Test
    void shouldReplaceOccurrencesAndRemoveOrphans() {
        EventEntity event = new EventEntity("event-789", "Concert", Instant.parse("2026-10-10T18:00:00Z"));

        event.addOccurrence(new EventOccurrenceEntity(Instant.parse("2026-10-10T18:00:00Z"), Instant.parse("2026-10-10T20:00:00Z")));

        event.addOccurrence(new EventOccurrenceEntity(Instant.parse("2026-10-11T18:00:00Z"), Instant.parse("2026-10-11T20:00:00Z")));

        repository.saveAndFlush(event);
        entityManager.clear();

        EventEntity persisted = repository.findById("event-789").orElseThrow();

        persisted.replaceOccurrences(List.of(new EventOccurrenceEntity(Instant.parse("2026-10-12T18:00:00Z"), Instant.parse("2026-10-12T20:00:00Z"))));

        repository.saveAndFlush(persisted);
        entityManager.clear();

        EventEntity updated = repository.findById("event-789").orElseThrow();

        assertThat(updated.getOccurrences()).singleElement().satisfies(occurrence -> assertThat(occurrence.getStart()).isEqualTo(Instant.parse("2026-10-12T18:00:00Z")));
    }
}
