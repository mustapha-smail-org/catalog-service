package com.citypulse.catalog.entity;

import com.citypulse.catalog.dto.request.FeedbackType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class FeedbackSubmissionEntity extends SubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private FeedbackType type;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    public FeedbackSubmissionEntity(FeedbackType type, String message, String email) {
        super(email);
        this.type = java.util.Objects.requireNonNull(type, "type must not be null");
        this.message = requireText(message, "message");
    }
}
