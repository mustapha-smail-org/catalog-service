package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventReportRequest;
import com.citypulse.catalog.dto.request.FeedbackSubmissionRequest;
import com.citypulse.catalog.dto.response.SubmissionResponse;
import com.citypulse.catalog.entity.EventReportEntity;
import com.citypulse.catalog.entity.FeedbackSubmissionEntity;
import com.citypulse.catalog.exception.EventNotFoundException;
import com.citypulse.catalog.repository.EventReportRepository;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.repository.FeedbackSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

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
}
