package com.citypulse.catalog.controller;

import com.citypulse.catalog.config.AdminTokenGuard;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventReportResponse;
import com.citypulse.catalog.dto.response.FeedbackSubmissionResponse;
import com.citypulse.catalog.exception.UnauthorizedException;
import com.citypulse.catalog.service.FeedbackService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackAdminControllerTest {

    private final FeedbackService feedbackService = mock(FeedbackService.class);
    private final AdminTokenGuard guard = mock(AdminTokenGuard.class);
    private final FeedbackAdminController controller = new FeedbackAdminController(feedbackService, guard);

    @Test
    void shouldGuardThenDelegateFeedbackListing() {
        CursorPageResponse<FeedbackSubmissionResponse> expected = new CursorPageResponse<>(List.of(), null, false);
        doNothing().when(guard).requireValidToken("s3cret");
        when(feedbackService.listFeedback(0, 20)).thenReturn(expected);

        assertThat(controller.listFeedback("s3cret", 0, 20)).isSameAs(expected);
        verify(guard).requireValidToken("s3cret");
    }

    @Test
    void shouldGuardThenDelegateReportListing() {
        CursorPageResponse<EventReportResponse> expected = new CursorPageResponse<>(List.of(), null, false);
        doNothing().when(guard).requireValidToken("s3cret");
        when(feedbackService.listReports(1, 50)).thenReturn(expected);

        assertThat(controller.listReports("s3cret", 1, 50)).isSameAs(expected);
        verify(guard).requireValidToken("s3cret");
    }

    @Test
    void shouldNotQueryFeedbackWhenTokenRejected() {
        doThrow(new UnauthorizedException("Invalid admin token")).when(guard).requireValidToken("bad");

        assertThatThrownBy(() -> controller.listFeedback("bad", 0, 20))
                .isInstanceOf(UnauthorizedException.class);
        verify(feedbackService, never()).listFeedback(0, 20);
    }
}
