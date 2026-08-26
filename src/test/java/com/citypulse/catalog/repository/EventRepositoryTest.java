package com.citypulse.catalog.repository;

import com.citypulse.catalog.entity.*;
import com.citypulse.catalog.service.EventSearchCriteria;
import com.citypulse.catalog.specification.EventSpecification;
import com.citypulse.catalog.utils.DateRange;
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
    private EventEnrichmentRepository enrichmentRepository;
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
    void periodShouldMatchOccurrenceDaysNotTheWholeEnvelope() {
        // Weekly event running only on Fridays 06/13/20 March 2026; its
        // envelope spans all three weeks, but it must only match on Fridays.
        EventEntity weekly = new EventEntity(
                "weekly-friday", "Friday concert",
                Instant.parse("2026-03-06T18:00:00Z")
        );
        weekly.setEndDate(Instant.parse("2026-03-20T20:00:00Z"));
        weekly.addOccurrence(new EventOccurrenceEntity(
                Instant.parse("2026-03-06T18:00:00Z"),
                Instant.parse("2026-03-06T20:00:00Z")));
        weekly.addOccurrence(new EventOccurrenceEntity(
                Instant.parse("2026-03-13T18:00:00Z"),
                Instant.parse("2026-03-13T20:00:00Z")));
        weekly.addOccurrence(new EventOccurrenceEntity(
                Instant.parse("2026-03-20T18:00:00Z"),
                Instant.parse("2026-03-20T20:00:00Z")));

        // Single-shot event on Tuesday 10 March with no stored occurrences:
        // must fall back to its start/end envelope.
        EventEntity oneShot = new EventEntity(
                "one-shot-tuesday", "Tuesday talk",
                Instant.parse("2026-03-10T19:00:00Z")
        );
        oneShot.setEndDate(Instant.parse("2026-03-10T21:00:00Z"));

        repository.saveAllAndFlush(List.of(weekly, oneShot));
        entityManager.clear();

        // Tuesday 10 March: weekly has no occurrence, only the one-shot matches.
        assertThat(idsMatching(day("2026-03-10")))
                .containsExactly("one-shot-tuesday");

        // Friday 13 March: the weekly occurrence matches; the one-shot does not.
        assertThat(idsMatching(day("2026-03-13")))
                .containsExactly("weekly-friday");

        // A range spanning all three Fridays must return the weekly event
        // exactly once (EXISTS, not a row-multiplying join) alongside the
        // one-shot, whose envelope also falls in the window.
        DateRange wholeSpan = new DateRange(
                Instant.parse("2026-03-06T00:00:00Z"),
                Instant.parse("2026-03-21T00:00:00Z"));
        assertThat(idsMatching(wholeSpan))
                .containsExactly("one-shot-tuesday", "weekly-friday");
    }

    private DateRange day(String isoDate) {
        Instant start = java.time.LocalDate.parse(isoDate)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return new DateRange(start, start.plus(java.time.Duration.ofDays(1)));
    }

    private List<String> idsMatching(DateRange range) {
        EventSearchCriteria criteria = new EventSearchCriteria(
                range, null, null, null, null, null, null);
        return repository.findAll(EventSpecification.matching(criteria))
                .stream().map(EventEntity::getId).sorted().toList();
    }

    @Test
    void shouldPersistAndReloadEnrichmentWithArrays() {
        EventEntity event = new EventEntity(
                "event-enriched", "Rooftop techno",
                Instant.parse("2026-08-20T18:00:00Z"));
        repository.saveAndFlush(event);

        EventEnrichmentEntity enrichment = new EventEnrichmentEntity(event);
        enrichment.setNormCategories(List.of("CLUBBING", "CONCERT"));
        enrichment.setMoodAffinities(List.of("FESTIF", "UNDERGROUND"));
        enrichment.setSocialContexts(List.of("ENTRE_AMIS"));
        enrichment.setSemanticTags(List.of("rooftop", "techno"));
        enrichment.setEnergyLevel("INTENSE");
        enrichment.setUniquenessScore(72);
        enrichment.setQualityScore(64);
        enrichment.setEnrichmentModel("test-model");
        enrichment.setEnrichmentVersion(1);
        enrichment.setEnrichmentSourceVersion(Instant.parse("2026-08-13T09:00:00Z"));
        enrichment.setEnrichedAt(Instant.parse("2026-08-14T09:00:00Z"));
        enrichmentRepository.saveAndFlush(enrichment);
        entityManager.clear();

        EventEnrichmentEntity reloaded =
                enrichmentRepository.findById("event-enriched").orElseThrow();

        assertThat(reloaded.getNormCategories())
                .containsExactly("CLUBBING", "CONCERT");
        assertThat(reloaded.getMoodAffinities())
                .containsExactly("FESTIF", "UNDERGROUND");
        assertThat(reloaded.getSemanticTags()).containsExactly("rooftop", "techno");
        assertThat(reloaded.getEnergyLevel()).isEqualTo("INTENSE");
        assertThat(reloaded.getUniquenessScore()).isEqualTo(72);
        assertThat(reloaded.getEnvironmentFallback()).isNull();
        assertThat(reloaded.getEvent().getId()).isEqualTo("event-enriched");
    }

    @Test
    void findIdsNeedingEnrichmentSelectsMissingStaleAndOutdated() {
        Instant sourceV1 = Instant.parse("2026-08-13T09:00:00Z");

        // needs: never enriched
        EventEntity missing = new EventEntity("need-missing", "Missing", sourceV1);
        missing.setSourceUpdatedAt(sourceV1);
        // needs: enriched at an older prompt version
        EventEntity oldVersion = new EventEntity("need-oldver", "Old version", sourceV1);
        oldVersion.setSourceUpdatedAt(sourceV1);
        // needs: source changed since it was enriched
        EventEntity stale = new EventEntity("need-stale", "Stale source", sourceV1);
        stale.setSourceUpdatedAt(Instant.parse("2026-08-20T09:00:00Z"));
        // up to date: same version, source matches -> must NOT be selected
        EventEntity current = new EventEntity("skip-current", "Current", sourceV1);
        current.setSourceUpdatedAt(sourceV1);
        repository.saveAllAndFlush(List.of(missing, oldVersion, stale, current));

        enrichmentRepository.saveAllAndFlush(List.of(
                enrichmentFor(oldVersion, 0, sourceV1),
                enrichmentFor(stale, 1, sourceV1),
                enrichmentFor(current, 1, sourceV1)));
        entityManager.clear();

        assertThat(repository.findIdsNeedingEnrichment(1, 50))
                .containsExactlyInAnyOrder("need-missing", "need-oldver", "need-stale")
                .doesNotContain("skip-current");
    }

    private EventEnrichmentEntity enrichmentFor(EventEntity event, int version, Instant sourceVersion) {
        EventEnrichmentEntity e = new EventEnrichmentEntity(event);
        e.setEnrichmentModel("test");
        e.setEnrichmentVersion(version);
        e.setEnrichmentSourceVersion(sourceVersion);
        e.setEnrichedAt(Instant.parse("2026-08-14T09:00:00Z"));
        return e;
    }

    @Test
    void eventWithoutEnrichmentReportsNull() {
        EventEntity event = new EventEntity(
                "event-bare", "No enrichment yet",
                Instant.parse("2026-08-20T18:00:00Z"));
        repository.saveAndFlush(event);
        entityManager.clear();

        EventEntity reloaded = repository.findById("event-bare").orElseThrow();
        assertThat(reloaded.getEnrichment()).isNull();
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
