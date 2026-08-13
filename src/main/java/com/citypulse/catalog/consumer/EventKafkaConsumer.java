package com.citypulse.catalog.consumer;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.EventIdentityConflictException;
import com.citypulse.catalog.exception.InvalidKafkaEventException;
import com.citypulse.catalog.exception.KafkaEventProcessingException;
import com.citypulse.catalog.mapper.EventAvroMapper;
import com.citypulse.catalog.service.EventIngestionService;
import com.citypulse.catalog.service.IngestionResult;
import com.citypulse.events.avro.EventAvro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaConsumer {

    private final EventAvroMapper eventAvroMapper;
    private final EventIngestionService eventIngestionService;

    @KafkaListener(topics = "${app.kafka.topic.events}", containerFactory = "eventKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, EventAvro> record, Acknowledgment acknowledgment) {
        EventAvro payload = record.value();
        String eventId = payload == null ? "unknown" : payload.getId();

        try {
            validatePayload(payload, eventId);

            EventEntity event = eventAvroMapper.toEntity(payload);
            IngestionResult result = eventIngestionService.ingest(event);

            acknowledgment.acknowledge();

            log.info("Event processed id={}, status={}, partition={}, offset={}", result.eventId(), result.status(), record.partition(), record.offset());
        } catch (InvalidKafkaEventException | EventIdentityConflictException |
                 DataIntegrityViolationException exception) {
            log.warn("Non-retryable Kafka event failure id={}, partition={}, offset={}", eventId, record.partition(), record.offset(), exception);

            throw exception;
        } catch (Exception exception) {
            log.error("Retryable Kafka event failure id={}, partition={}, offset={}", eventId, record.partition(), record.offset(), exception);
            throw new KafkaEventProcessingException(eventId, exception);
        }
    }

    private void validatePayload(EventAvro payload, String eventId) {
        if (payload == null) {
            throw new InvalidKafkaEventException(eventId, "payload is null");
        }

        if (payload.getId() == null || payload.getId().isBlank()) {
            throw new InvalidKafkaEventException(eventId, "id is required");
        }

        if (payload.getTitle() == null || payload.getTitle().isBlank()) {
            throw new InvalidKafkaEventException(eventId, "title is required");
        }

        if (payload.getStartDate() == null) {
            throw new InvalidKafkaEventException(eventId, "startDate is required");
        }

        if (payload.getLocation() == null || payload.getLocation().getName() == null || payload.getLocation().getName().isBlank()) {
            throw new InvalidKafkaEventException(eventId, "location is required");
        }
    }
}