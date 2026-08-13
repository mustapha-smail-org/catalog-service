package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.entity.EventAccessibilityEmbeddable;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventLocationEmbeddable;
import com.citypulse.catalog.entity.EventPricingEmbeddable;
import com.citypulse.catalog.repository.EventRepository;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void shouldReturnSerializableEventSummariesWithCategories() throws Exception {
        EventEntity event = serializableEvent(
                "event-serializable",
                "Serializable event",
                Instant.parse("2026-09-01T18:00:00Z")
        );

        eventRepository.saveAndFlush(event);

        CursorPageResponse<EventSummaryResponse> response = eventQueryService.findEvents(
                new EventSearchRequest(null, null, null, null, null, null, 10, null)
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
