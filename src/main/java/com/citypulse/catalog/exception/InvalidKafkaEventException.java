package com.citypulse.catalog.exception;

public class InvalidKafkaEventException extends RuntimeException {

    public InvalidKafkaEventException(
            String eventId,
            String reason,
            Throwable cause
    ) {
        super(
                "Invalid Kafka event id=%s: %s"
                        .formatted(eventId, reason),
                cause
        );
    }

    public InvalidKafkaEventException(
            String eventId,
            String reason
    ) {
        this(eventId, reason, null);
    }
}