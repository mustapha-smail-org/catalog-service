package com.citypulse.catalog.consumer;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.KafkaEventProcessingException;
import com.citypulse.catalog.mapper.EventAvroMapper;
import com.citypulse.catalog.service.EventIngestionService;
import com.citypulse.catalog.service.IngestionResult;
import com.citypulse.catalog.service.IngestionStatus;
import com.citypulse.events.avro.EventAccessibilityAvro;
import com.citypulse.events.avro.EventAvro;
import com.citypulse.events.avro.EventLocationAvro;
import com.citypulse.events.avro.EventPricingAvro;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventKafkaConsumerTest {

    @Mock
    private EventAvroMapper mapper;

    @Mock
    private EventIngestionService ingestionService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private EventKafkaConsumer consumer;

    private static EventAvro getEventAvro() {
        EventPricingAvro eventPricingAvro = EventPricingAvro.newBuilder().setPriceDetail("").setPriceType("").setAccessType("").setBookingUrl("").setBookingLinkText("").build();
        EventAccessibilityAvro accessibilityAvro = EventAccessibilityAvro.newBuilder().setMentalAccessibility("").setSignLanguage("").setBlindAccessible(false).setDeafAccessible(false).setWheelchairAccessible(false).build();
        EventLocationAvro locationAvro = EventLocationAvro.newBuilder().setName("").setStreet("").setZipcode("").setCity("").setLatitude(0.0).setLongitude(0.0).build();
        return EventAvro.newBuilder().setId("event-123").setTitle("Paris concert").setStartDate(Instant.parse("2026-08-10T18:00:00Z")).setPricing(eventPricingAvro).setLocation(locationAvro).setAccessibility(accessibilityAvro).build();
    }

    @Test
    void shouldPersistAndAcknowledgeEvent() {
        EventAvro payload = getEventAvro();

        EventEntity entity = new EventEntity("event-123", "Paris concert", Instant.parse("2026-08-10T18:00:00Z"));

        ConsumerRecord<String, EventAvro> record = new ConsumerRecord<>("citypulse.events.v1", 0, 10L, "event-123", payload);

        when(mapper.toEntity(payload)).thenReturn(entity);
        when(ingestionService.ingest(entity)).thenReturn(new IngestionResult("event-123", IngestionStatus.INSERTED, Instant.parse("2026-08-10T12:00:00Z")));
        consumer.consume(record, acknowledgment);

        verify(mapper).toEntity(payload);
        verify(ingestionService).ingest(entity);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenPersistenceFails() {
        EventAvro payload = getEventAvro();

        EventEntity entity = new EventEntity("event-123", "Paris concert", Instant.parse("2026-08-10T18:00:00Z"));

        ConsumerRecord<String, EventAvro> record = new ConsumerRecord<>("citypulse.events.v1", 0, 10L, "event-123", payload);

        when(mapper.toEntity(payload)).thenReturn(entity);

        doThrow(new RuntimeException("Database unavailable")).when(ingestionService).ingest(entity);

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment)).isInstanceOf(KafkaEventProcessingException.class).hasMessageContaining("event-123");

        verify(acknowledgment, never()).acknowledge();
    }
}