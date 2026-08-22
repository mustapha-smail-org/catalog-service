package com.citypulse.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import com.citypulse.catalog.dto.request.EventReportType;

@Getter
@Setter
@Entity
@Table(
        name = "event_reports",
        indexes = {
                @Index(name = "idx_event_reports_event_slug", columnList = "event_slug"),
                @Index(name = "idx_event_reports_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_slug", nullable = false, length = 220)
    private String eventSlug;

    @Column(name = "event_id", nullable = false, length = 150)
    private String eventId;

    @Column(name = "event_title", nullable = false, length = 500)
    private String eventTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private EventReportType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.OPEN;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EventReportEntity(String eventId, String eventSlug, String eventTitle, EventReportType type, String message, String email) {
        this.eventId = requireText(eventId, "eventId");
        this.eventSlug = requireText(eventSlug, "eventSlug");
        this.eventTitle = requireText(eventTitle, "eventTitle");
        this.type = java.util.Objects.requireNonNull(type, "type must not be null");
        this.message = blankToNull(message);
        this.email = blankToNull(email);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
