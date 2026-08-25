package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventReportRequest;
import com.citypulse.catalog.dto.request.FeedbackSubmissionRequest;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventReportResponse;
import com.citypulse.catalog.dto.response.FeedbackSubmissionResponse;
import com.citypulse.catalog.dto.response.SubmissionResponse;
import com.citypulse.catalog.entity.EventReportEntity;
import com.citypulse.catalog.entity.FeedbackSubmissionEntity;
import com.citypulse.catalog.exception.EventNotFoundException;
import com.citypulse.catalog.repository.EventReportRepository;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.repository.FeedbackSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final EventRepository eventRepository;
    private final FeedbackSubmissionRepository feedbackRepository;
    private final EventReportRepository reportRepository;

    @Transactional
    public SubmissionResponse submitFeedback(FeedbackSubmissionRequest request) {
        FeedbackSubmissionEntity saved = feedbackRepository.save(
                new FeedbackSubmissionEntity(
                        request.type(),
                        request.message(),
                        request.email()
                )
        );

        return new SubmissionResponse(saved.getId().toString(), "RECEIVED");
    }

    @Transactional
    public SubmissionResponse reportEvent(String slug, EventReportRequest request) {
        var event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new EventNotFoundException(slug));

        EventReportEntity saved = reportRepository.save(
                new EventReportEntity(
                        event.getId(),
                        slug,
                        event.getTitle(),
                        request.type(),
                        request.message(),
                        request.email()
                )
        );

        return new SubmissionResponse(saved.getId().toString(), "RECEIVED");
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<FeedbackSubmissionResponse> listFeedback(Integer page, Integer size) {
        Page<FeedbackSubmissionEntity> result =
                feedbackRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size));

        return toPage(result, FeedbackSubmissionResponse::from);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<EventReportResponse> listReports(Integer page, Integer size) {
        Page<EventReportEntity> result =
                reportRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size));

        return toPage(result, EventReportResponse::from);
    }

    private static PageRequest pageRequest(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize);
    }

    private static <E, T> CursorPageResponse<T> toPage(Page<E> page, Function<E, T> mapper) {
        String nextCursor = page.hasNext() ? String.valueOf(page.getNumber() + 1) : null;

        return new CursorPageResponse<>(page.map(mapper).getContent(), nextCursor, page.hasNext());
    }
}
