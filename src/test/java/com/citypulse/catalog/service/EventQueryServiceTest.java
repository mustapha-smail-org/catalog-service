package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.request.PricingFilter;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventFacetsResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.dto.response.FacetCountResponse;
import com.citypulse.catalog.entity.EventAccessibilityEmbeddable;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventEnvironment;
import com.citypulse.catalog.entity.EventLocationEmbeddable;
import com.citypulse.catalog.entity.EventPricingEmbeddable;
import com.citypulse.catalog.repository.EventRepository;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class EventQueryServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("citypulse_catalog")
            .withUsername("citypulse")
            .withPassword("citypulse");

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventQueryService eventQueryService;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new SimpleModule().addSerializer(
                    OffsetDateTime.class,
                    new JsonSerializer<>() {
                        @Override
                        public void serialize(
                                OffsetDateTime value,
                                JsonGenerator gen,
                                SerializerProvider serializers
                        ) throws IOException {
                            gen.writeString(value.toString());
                        }
                    }
            ))
            .build();


    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
    }

    @Test
    void shouldReturnSerializableEventSummariesWithCategories() throws Exception {
        EventEntity event = serializableEvent(
                "event-serializable",
                "Serializable event",
                Instant.parse("2026-09-01T18:00:00Z")
        );

        eventRepository.saveAndFlush(event);

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(null, null, null, null, null, null, null, null, null, 10, null, null)
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("Cinema", "Outdoor");
    }

    @Test
    void shouldReturnSerializableEventDetailWithCategories() throws Exception {
        EventEntity event = serializableEvent(
                "event-detail-serializable",
                "Serializable detail event",
                Instant.parse("2026-09-02T18:00:00Z")
        );

        eventRepository.saveAndFlush(event);

        EventDetailResponse response = eventQueryService.findById(
                "event-detail-serializable"
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("Cinema", "Outdoor");
    }

    @Test
    void shouldMatchAnyOfMultipleCategories() {
        eventRepository.saveAndFlush(filterableEvent(
                "cat-concert", "Concert night", Instant.parse("2026-09-10T18:00:00Z"),
                "75001", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "cat-expo", "Expo day", Instant.parse("2026-09-11T18:00:00Z"),
                "75001", Set.of("Expositions")));
        eventRepository.saveAndFlush(filterableEvent(
                "cat-theatre", "Theatre play", Instant.parse("2026-09-12T18:00:00Z"),
                "75001", Set.of("Theatre")));

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(
                        null, null, List.of("Concerts", "Theatre"), null, null,
                        null, null, null, null, 50, null, null)
        );

        assertThat(response.items()).extracting(EventSummaryResponse::title)
                .containsExactlyInAnyOrder("Concert night", "Theatre play");
    }

    @Test
    void shouldMatchAnyOfMultipleArrondissements() {
        eventRepository.saveAndFlush(filterableEvent(
                "arr-1", "First arr event", Instant.parse("2026-09-10T18:00:00Z"),
                "75001", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "arr-15", "Fifteenth arr event", Instant.parse("2026-09-11T18:00:00Z"),
                "75015", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "arr-outside", "Outside Paris event", Instant.parse("2026-09-12T18:00:00Z"),
                "69000", Set.of("Concerts")));

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(
                        null, null, null, null, null,
                        List.of("1", "OUTSIDE_PARIS"), null, null, null, 50, null, null)
        );

        assertThat(response.items()).extracting(EventSummaryResponse::title)
                .containsExactlyInAnyOrder("First arr event", "Outside Paris event");
    }

    @Test
    void shouldFilterBySpecificDate() {
        eventRepository.saveAndFlush(filterableEvent(
                "date-target", "Target day event", Instant.parse("2026-09-01T18:00:00Z"),
                "75001", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "date-other", "Other day event", Instant.parse("2026-09-05T18:00:00Z"),
                "75001", Set.of("Concerts")));

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(
                        null, LocalDate.of(2026, 9, 1), null, null, null,
                        null, null, null, null, 50, null, null)
        );

        assertThat(response.items()).extracting(EventSummaryResponse::title)
                .containsExactly("Target day event");
    }

    @Test
    void shouldCountCategoriesWithDrillDownIgnoringSelectedCategoryButHonouringPricing() {
        eventRepository.saveAndFlush(filterableEvent(
                "fa-1", "Concert A", Instant.parse("2026-09-10T18:00:00Z"),
                "75001", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "fa-2", "Concert B", Instant.parse("2026-09-11T18:00:00Z"),
                "75002", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "fa-3", "Expo C", Instant.parse("2026-09-12T18:00:00Z"),
                "75001", Set.of("Expositions")));
        eventRepository.saveAndFlush(filterableEvent(
                "fa-4", "Paid concert D", Instant.parse("2026-09-13T18:00:00Z"),
                "75001", Set.of("Concerts"), "payant"));

        EventFacetsResponse facets = eventQueryService.findFacets(
                new EventSearchRequest(
                        null, null, List.of("Concerts"), null, PricingFilter.FREE,
                        null, null, null, null, null, null, null)
        );

        assertThat(facets.categories()).containsExactly(
                new FacetCountResponse("Concerts", 2L),
                new FacetCountResponse("Expositions", 1L)
        );
        assertThat(facets.arrondissements()).containsExactly(
                new FacetCountResponse("1", 1L),
                new FacetCountResponse("2", 1L)
        );
    }

    @Test
    void shouldCountArrondissementBucketsInOrder() {
        eventRepository.saveAndFlush(filterableEvent(
                "ab-1", "First A", Instant.parse("2026-09-10T18:00:00Z"),
                "75001", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "ab-2", "First B", Instant.parse("2026-09-11T18:00:00Z"),
                "75001", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "ab-15", "Fifteenth", Instant.parse("2026-09-12T18:00:00Z"),
                "75015", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "ab-outside", "Outside", Instant.parse("2026-09-13T18:00:00Z"),
                "69000", Set.of("Concerts")));
        eventRepository.saveAndFlush(filterableEvent(
                "ab-unknown", "Unknown", Instant.parse("2026-09-14T18:00:00Z"),
                null, Set.of("Concerts")));

        EventFacetsResponse facets = eventQueryService.findFacets(
                new EventSearchRequest(
                        null, null, null, null, null, null, null, null, null, null, null, null)
        );

        assertThat(facets.arrondissements()).containsExactly(
                new FacetCountResponse("1", 2L),
                new FacetCountResponse("15", 1L),
                new FacetCountResponse("OUTSIDE_PARIS", 1L),
                new FacetCountResponse("UNKNOWN", 1L)
        );
    }

    @Test
    void relevanceSortRanksByScoreNullsLastAndPaginatesStably() {
        Instant start = Instant.parse("2026-09-01T18:00:00Z");
        eventRepository.saveAll(List.of(
                ranked("evt-a", 0.9, start), ranked("evt-b", 0.5, start),
                ranked("evt-d", 0.5, start), ranked("evt-c", null, start)));

        // Order: 0.9, then the 0.5 tie by id asc (b, d), then the null tail (c).
        CursorPageResponse<EventSummaryResponse> page1 = findRelevance(2, null);
        assertThat(idsOf(page1)).containsExactly("evt-a", "evt-b");
        assertThat(page1.hasNext()).isTrue();

        CursorPageResponse<EventSummaryResponse> page2 =
                findRelevance(2, page1.nextCursor());
        assertThat(idsOf(page2)).containsExactly("evt-d", "evt-c");
        assertThat(page2.hasNext()).isFalse();
    }

    @Test
    void relevancePaginatesThroughTheUnenrichedTail() {
        Instant start = Instant.parse("2026-09-01T18:00:00Z");
        eventRepository.saveAll(List.of(
                ranked("evt-a", 0.9, start), ranked("evt-b", null, start),
                ranked("evt-c", null, start)));

        CursorPageResponse<EventSummaryResponse> p1 = findRelevance(1, null);
        assertThat(idsOf(p1)).containsExactly("evt-a");

        // Page 2 enters the null-rank tail; its cursor therefore carries a null
        // rank, exercising the null-cursor branch of the keyset on page 3.
        CursorPageResponse<EventSummaryResponse> p2 = findRelevance(1, p1.nextCursor());
        assertThat(idsOf(p2)).containsExactly("evt-b");

        CursorPageResponse<EventSummaryResponse> p3 = findRelevance(1, p2.nextCursor());
        assertThat(idsOf(p3)).containsExactly("evt-c");
        assertThat(p3.hasNext()).isFalse();
    }

    @Test
    void rejectsCursorReplayedUnderADifferentSort() {
        Instant start = Instant.parse("2026-09-01T18:00:00Z");
        eventRepository.saveAll(List.of(
                ranked("evt-a", 0.9, start), ranked("evt-b", 0.5, start)));

        CursorPageResponse<EventSummaryResponse> startDatePage =
                eventQueryService.findEvents(new EventSearchRequest(
                        null, null, null, null, null, null, null, null,
                        com.citypulse.catalog.dto.request.EventSort.START_DATE, 1, null, null));
        String startDateCursor = startDatePage.nextCursor();

        assertThatThrownBy(() -> findRelevance(1, startDateCursor))
                .isInstanceOf(com.citypulse.catalog.exception.InvalidCursorException.class);
    }

    @Test
    void freePricingFilterIncludesConditionallyFreeEvents() {
        Instant start = Instant.parse("2026-09-01T18:00:00Z");
        eventRepository.saveAll(List.of(
                filterableEvent("free-1", "Free", start, "75001", Set.of("Concerts"), "gratuit"),
                filterableEvent("cond-1", "Conditional", start, "75001", Set.of("Concerts"), "gratuit sous condition"),
                filterableEvent("paid-1", "Paid", start, "75001", Set.of("Concerts"), "12 EUR")));

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(null, null, null, null, PricingFilter.FREE,
                        null, null, null, null, 10, null, null));

        assertThat(response.items()).extracting(EventSummaryResponse::id)
                .containsExactlyInAnyOrder("free-1", "cond-1");
        assertThat(response.items()).filteredOn(e -> e.id().equals("cond-1"))
                .singleElement().extracting(EventSummaryResponse::pricing)
                .isEqualTo("FREE_CONDITIONAL");
    }

    @Test
    void environmentFilterMatchesOnlyTheRequestedSetting() {
        Instant start = Instant.parse("2026-09-01T18:00:00Z");
        EventEntity indoor = filterableEvent("env-indoor", "Indoor", start, "75001", Set.of("Concerts"));
        indoor.setEnvironment(EventEnvironment.INDOOR);
        EventEntity outdoor = filterableEvent("env-outdoor", "Outdoor", start, "75001", Set.of("Concerts"));
        outdoor.setEnvironment(EventEnvironment.OUTDOOR);
        eventRepository.saveAll(List.of(indoor, outdoor));

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(null, null, null, null, null, null, null, null,
                        null, 10, null, "OUTDOOR"));

        assertThat(response.items()).extracting(EventSummaryResponse::id)
                .containsExactly("env-outdoor");
    }

    private EventEntity ranked(String id, Double rankScore, Instant startDate) {
        EventEntity event = new EventEntity(id, id.toUpperCase(), startDate);
        event.setLocation(new EventLocationEmbeddable(
                "Test venue", "1 Test street", "75001", "Paris", 48.8566, 2.3522));
        event.setPricing(new EventPricingEmbeddable(
                "gratuit", "Detail", "libre", null, null));
        event.setRankScore(rankScore);
        return event;
    }

    private CursorPageResponse<EventSummaryResponse> findRelevance(int limit, String cursor) {
        return eventQueryService.findEvents(new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                com.citypulse.catalog.dto.request.EventSort.RELEVANCE, limit, cursor, null));
    }

    private List<String> idsOf(CursorPageResponse<EventSummaryResponse> page) {
        return page.items().stream().map(EventSummaryResponse::id).toList();
    }

    private EventEntity filterableEvent(
            String id,
            String title,
            Instant startDate,
            String zipcode,
            Set<String> categories
    ) {
        return filterableEvent(id, title, startDate, zipcode, categories, "gratuit");
    }

    private EventEntity filterableEvent(
            String id,
            String title,
            Instant startDate,
            String zipcode,
            Set<String> categories,
            String priceType
    ) {
        EventEntity event = new EventEntity(id, title, startDate);
        event.setLocation(new EventLocationEmbeddable(
                "Test venue", "1 Test street", zipcode, "Paris", 48.8566, 2.3522
        ));
        event.setPricing(new EventPricingEmbeddable(
                priceType, "Detail", "libre", null, null
        ));
        event.replaceCategories(categories);

        return event;
    }

    private EventEntity serializableEvent(
            String id,
            String title,
            Instant startDate
    ) {
        EventEntity event = new EventEntity(id, title, startDate);
        event.setLocation(new EventLocationEmbeddable(
                "Test venue",
                "1 Test street",
                "75001",
                "Paris",
                48.8566,
                2.3522
        ));
        event.setAccessibility(new EventAccessibilityEmbeddable(
                true,
                false,
                false,
                null,
                null
        ));
        event.setPricing(new EventPricingEmbeddable(
                "gratuit",
                "Free",
                "libre",
                null,
                null
        ));
        event.replaceCategories(Set.of("Cinema", "Outdoor"));

        return event;
    }
}
