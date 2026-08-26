package com.citypulse.catalog.enrichment;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventLocationEmbeddable;
import com.citypulse.catalog.entity.EventPricingEmbeddable;
import com.citypulse.catalog.repository.EventEnrichmentRepository;
import com.citypulse.catalog.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link EnrichmentStore} as a real (proxied) bean with the test
 * running OUTSIDE a transaction, so each store call gets its own session — the
 * exact shape that surfaced the LazyInitializationException on
 * {@code EventEntity.categories}.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class EnrichmentStoreIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("citypulse_catalog")
            .withUsername("citypulse")
            .withPassword("citypulse");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private EnrichmentStore store;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventEnrichmentRepository enrichmentRepository;

    @BeforeEach
    void clean() {
        enrichmentRepository.deleteAll();
        eventRepository.deleteAll();
    }

    private EnrichmentResult result() {
        return new EnrichmentResult(
                List.of("CONCERT", "CLUBBING"), List.of("FESTIF"),
                List.of("ENTRE_AMIS"), List.of("techno"), "INTENSE", null, 60, 70);
    }

    @Test
    void loadInputInitialisesLazyCategoriesOutsideAnAmbientTransaction() {
        EventEntity event = new EventEntity("evt-1", "Rooftop", Instant.parse("2026-09-01T18:00:00Z"));
        event.setLeadText("Sur les toits");
        event.replaceCategories(Set.of("Concert", "Nuit"));
        event.setLocation(new EventLocationEmbeddable("Le Rooftop", "1 rue", "75011", "Paris", 48.8, 2.3));
        event.setPricing(new EventPricingEmbeddable("gratuit", null, null, null, null));
        eventRepository.saveAndFlush(event);

        EnrichmentInput input = store.loadInput("evt-1").orElseThrow();

        assertThat(input.rawCategories()).containsExactlyInAnyOrder("Concert", "Nuit");
        assertThat(input.venue()).isEqualTo("Le Rooftop");
        assertThat(input.arrondissement()).isEqualTo(11);
        assertThat(input.priceType()).isEqualTo("gratuit");
    }

    @Test
    void savePersistsEnrichmentAndDenormalisesRankOntoEvent() {
        EventEntity event = new EventEntity("evt-2", "Concert", Instant.parse("2026-09-01T18:00:00Z"));
        event.setSourceUpdatedAt(Instant.parse("2026-08-13T09:00:00Z"));
        eventRepository.saveAndFlush(event);

        store.save("evt-2", result());

        double expectedRank = EnrichmentRankScorer.score(60, 70);
        assertThat(enrichmentRepository.findById("evt-2")).get().satisfies(e -> {
            assertThat(e.getNormCategories()).containsExactly("CONCERT", "CLUBBING");
            assertThat(e.getRankScore()).isEqualTo(expectedRank);
            assertThat(e.getEnrichmentVersion()).isEqualTo(1);
            assertThat(e.getEnrichmentSourceVersion()).isEqualTo(Instant.parse("2026-08-13T09:00:00Z"));
        });
        assertThat(eventRepository.findById("evt-2")).get()
                .extracting(EventEntity::getRankScore).isEqualTo(expectedRank);
    }
}
