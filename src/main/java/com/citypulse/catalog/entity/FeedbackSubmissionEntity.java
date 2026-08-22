package com.citypulse.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import com.citypulse.catalog.dto.request.FeedbackType;

@Getter
@Setter
@Entity
@Table(
        name = "feedback_submissions",
        indexes = {
                @Index(name = "idx_feedback_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private FeedbackType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.OPEN;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FeedbackSubmissionEntity(FeedbackType type, String message, String email) {
        this.type = java.util.Objects.requireNonNull(type, "type must not be null");
        this.message = requireText(message, "message");
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
