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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
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

    @Test
    void shouldMapFeedbackPageAndExposeNextCursorWhenMoreExist() {
        FeedbackSubmissionEntity entity = new FeedbackSubmissionEntity(
                FeedbackType.BUG, "Map does not load", "reader@example.com"
        );
        entity.setId(7L);
        entity.setCreatedAt(Instant.parse("2026-08-20T10:00:00Z"));
        // total 45 rows over pages of 20 -> page 0 has a next page
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 45));

        var page = service.listFeedback(0, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().id()).isEqualTo("7");
        assertThat(page.items().getFirst().type()).isEqualTo(FeedbackType.BUG);
        assertThat(page.items().getFirst().email()).isEqualTo("reader@example.com");
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo("1");
    }

    @Test
    void shouldClampFeedbackPageSizeAndDefaultNullArguments() {
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.listFeedback(null, 5_000);

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(feedbackRepository).findAllByOrderByCreatedAtDesc(pageRequest.capture());
        assertThat(pageRequest.getValue().getPageNumber()).isZero();
        assertThat(pageRequest.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void shouldMapReportPageWithoutNextCursorOnLastPage() {
        EventReportEntity entity = new EventReportEntity(
                "event-42", "open-air-cinema-a1b2c3d4", "Open Air Cinema",
                EventReportType.BROKEN_LINK, "Ticket link is dead", null
        );
        entity.setId(9L);
        entity.setCreatedAt(Instant.parse("2026-08-21T09:00:00Z"));
        when(reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));

        var page = service.listReports(null, null);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().eventTitle()).isEqualTo("Open Air Cinema");
        assertThat(page.items().getFirst().type()).isEqualTo(EventReportType.BROKEN_LINK);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }
}
