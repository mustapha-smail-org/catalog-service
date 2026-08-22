package com.citypulse.catalog.entity;

import com.citypulse.catalog.dto.request.EventReportType;
import com.citypulse.catalog.dto.request.FeedbackType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionEntityTest {

    @Test
    void shouldNormalizeFeedbackAndInitializeLifecycleFields() {
        FeedbackSubmissionEntity feedback = new FeedbackSubmissionEntity(
                FeedbackType.GENERAL, "  Great app  ", "  reader@example.com  "
        );
        Instant beforePersist = Instant.now();

        feedback.prePersist();

        assertThat(feedback.getType()).isEqualTo(FeedbackType.GENERAL);
        assertThat(feedback.getMessage()).isEqualTo("Great app");
        assertThat(feedback.getEmail()).isEqualTo("reader@example.com");
        assertThat(feedback.getStatus()).isEqualTo(SubmissionStatus.OPEN);
        assertThat(feedback.getCreatedAt()).isAfterOrEqualTo(beforePersist);
    }

    @Test
    void shouldNormalizeOptionalReportFields() {
        EventReportEntity report = new EventReportEntity(
                "  event-42  ",
                "  event-slug  ",
                "  Event title  ",
                EventReportType.BROKEN_LINK,
                "  ",
                null
        );

        assertThat(report.getEventId()).isEqualTo("event-42");
        assertThat(report.getEventSlug()).isEqualTo("event-slug");
        assertThat(report.getEventTitle()).isEqualTo("Event title");
        assertThat(report.getType()).isEqualTo(EventReportType.BROKEN_LINK);
        assertThat(report.getMessage()).isNull();
        assertThat(report.getEmail()).isNull();
    }

    @Test
    void shouldRejectMissingRequiredFeedbackValues() {
        assertThatThrownBy(() -> new FeedbackSubmissionEntity(null, "message", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
        assertThatThrownBy(() -> new FeedbackSubmissionEntity(FeedbackType.BUG, "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message must not be blank");
    }

    @Test
    void shouldRejectMissingRequiredReportValues() {
        assertThatThrownBy(() -> new EventReportEntity(
                null, "slug", "title", EventReportType.EVENT_CANCELLED, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must not be blank");
        assertThatThrownBy(() -> new EventReportEntity(
                "id", "slug", "title", null, null, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }
}
