package com.citypulse.catalog.exception;

public class KafkaEventProcessingException extends RuntimeException {

    public KafkaEventProcessingException(String eventId, Throwable cause) {
        super("Failed to process Kafka event: " + eventId, cause);
    }
}