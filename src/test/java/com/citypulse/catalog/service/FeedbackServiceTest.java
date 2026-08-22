package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventReportRequest;
import com.citypulse.catalog.dto.request.EventReportType;
import com.citypulse.catalog.dto.request.FeedbackSubmissionRequest;
import com.citypulse.catalog.dto.request.FeedbackType;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventReportEntity;
import com.citypulse.catalog.entity.FeedbackSubmissionEntity;
import com.citypulse.catalog.exception.EventNotFoundException;
import com.citypulse.catalog.repository.EventReportRepository;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.repository.FeedbackSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private FeedbackSubmissionRepository feedbackRepository;
    @Mock
    private EventReportRepository reportRepository;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(eventRepository, feedbackRepository, reportRepository);
    }

    @Test
    void shouldPersistFeedbackAndReturnSubmissionId() {
        FeedbackSubmissionRequest request = new FeedbackSubmissionRequest(
                FeedbackType.CONTENT, "  Add more exhibitions  ", "  reader@example.com  "
        );
        when(feedbackRepository.save(any(FeedbackSubmissionEntity.class))).thenAnswer(invocation -> {
            FeedbackSubmissionEntity saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        var response = service.submitFeedback(request);

        assertThat(response.id()).isEqualTo("12");
        assertThat(response.status()).isEqualTo("RECEIVED");
        ArgumentCaptor<FeedbackSubmissionEntity> submission = ArgumentCaptor.forClass(FeedbackSubmissionEntity.class);
        verify(feedbackRepository).save(submission.capture());
        assertThat(submission.getValue().getMessage()).isEqualTo("Add more exhibitions");
        assertThat(submission.getValue().getEmail()).isEqualTo("reader@example.com");
    }

    @Test
    void shouldPersistReportWithEventSnapshot() {
        EventEntity event = new EventEntity("event-42", "Open Air Cinema", Instant.parse("2026-08-20T18:00:00Z"));
        EventReportRequest request = new EventReportRequest(
                EventReportType.INCORRECT_INFORMATION, "  Wrong opening time  ", null
        );
        when(eventRepository.findBySlug(event.getSlug())).thenReturn(Optional.of(event));
        when(reportRepository.save(any(EventReportEntity.class))).thenAnswer(invocation -> {
            EventReportEntity saved = invocation.getArgument(0);
            saved.setId(24L);
            return saved;
        });

        var response = service.reportEvent(event.getSlug(), request);

        assertThat(response.id()).isEqualTo("24");
        assertThat(response.status()).isEqualTo("RECEIVED");
        ArgumentCaptor<EventReportEntity> report = ArgumentCaptor.forClass(EventReportEntity.class);
        verify(reportRepository).save(report.capture());
        assertThat(report.getValue().getEventId()).isEqualTo("event-42");
        assertThat(report.getValue().getEventSlug()).isEqualTo(event.getSlug());
        assertThat(report.getValue().getEventTitle()).isEqualTo("Open Air Cinema");
        assertThat(report.getValue().getMessage()).isEqualTo("Wrong opening time");
    }

    @Test
    void shouldRejectReportForUnknownEvent() {
        when(eventRepository.findBySlug("missing")).thenReturn(Optional.empty());
        EventReportRequest request = new EventReportRequest(EventReportType.BROKEN_LINK, null, null);

        assertThatThrownBy(() -> service.reportEvent("missing", request))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("missing");
    }
}
