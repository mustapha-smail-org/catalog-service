package com.citypulse.catalog.controller;

import com.citypulse.catalog.dto.request.EventReportRequest;
import com.citypulse.catalog.dto.request.EventReportType;
import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.request.FeedbackSubmissionRequest;
import com.citypulse.catalog.dto.request.FeedbackType;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.dto.response.SubmissionResponse;
import com.citypulse.catalog.service.EventQueryService;
import com.citypulse.catalog.service.FeedbackService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventControllerTest {

    private final EventQueryService service = mock(EventQueryService.class);
    private final FeedbackService feedbackService = mock(FeedbackService.class);
    private final EventController controller = new EventController(service, feedbackService);
    private final EventSearchRequest request = new EventSearchRequest(
            null, null, null, null, null, null, null, null
    );

    @Test
    void shouldDelegateEventSearch() {
        CursorPageResponse<EventSummaryResponse> expected = new CursorPageResponse<>(List.of(), null, false);
        when(service.findEvents(request)).thenReturn(expected);

        assertThat(controller.findEvents(request)).isSameAs(expected);
    }

    @Test
    void shouldDelegateMapSearch() {
        CursorPageResponse<EventMapMarkerResponse> expected = new CursorPageResponse<>(List.of(), null, false);
        when(service.findMapEvents(request)).thenReturn(expected);

        assertThat(controller.findMapEvents(request)).isSameAs(expected);
    }

    @Test
    void shouldDelegateDetailLookup() {
        EventDetailResponse expected = mock(EventDetailResponse.class);
        when(service.findById("event-42")).thenReturn(expected);

        assertThat(controller.findEvent("event-42")).isSameAs(expected);
    }

    @Test
    void shouldDelegateSlugLookup() {
        EventDetailResponse expected = mock(EventDetailResponse.class);
        when(service.findBySlug("open-air-cinema-a1b2c3d4")).thenReturn(expected);

        assertThat(controller.findEventBySlug("open-air-cinema-a1b2c3d4")).isSameAs(expected);
    }

    @Test
    void shouldDelegateFeedbackSubmission() {
        FeedbackSubmissionRequest feedbackRequest = new FeedbackSubmissionRequest(
                FeedbackType.GENERAL, "Great app", "reader@example.com"
        );
        SubmissionResponse expected = new SubmissionResponse("1", "RECEIVED");
        when(feedbackService.submitFeedback(feedbackRequest)).thenReturn(expected);

        assertThat(controller.submitFeedback(feedbackRequest)).isSameAs(expected);
    }

    @Test
    void shouldDelegateEventReport() {
        EventReportRequest reportRequest = new EventReportRequest(
                EventReportType.BROKEN_LINK, "The official URL is unavailable", "reader@example.com"
        );
        SubmissionResponse expected = new SubmissionResponse("2", "RECEIVED");
        when(feedbackService.reportEvent("event-slug", reportRequest)).thenReturn(expected);

        assertThat(controller.reportEvent("event-slug", reportRequest)).isSameAs(expected);
    }

    @Test
    void shouldDelegateCategoryLookup() {
        when(service.findCategories()).thenReturn(List.of("Cinema", "Music"));

        assertThat(controller.findCategories()).containsExactly("Cinema", "Music");
        verify(service).findCategories();
    }
}
