package com.citypulse.catalog.entity;

import com.citypulse.catalog.dto.request.EventReportType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class EventReportEntity extends SubmissionEntity {

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

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    public EventReportEntity(String eventId, String eventSlug, String eventTitle, EventReportType type, String message, String email) {
        super(email);
        this.eventId = requireText(eventId, "eventId");
        this.eventSlug = requireText(eventSlug, "eventSlug");
        this.eventTitle = requireText(eventTitle, "eventTitle");
        this.type = java.util.Objects.requireNonNull(type, "type must not be null");
        this.message = blankToNull(message);
    }
}
