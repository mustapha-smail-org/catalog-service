package com.citypulse.catalog.controller;

import com.citypulse.catalog.dto.request.EventReportRequest;
import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.request.FeedbackSubmissionRequest;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.dto.response.SubmissionResponse;
import com.citypulse.catalog.service.EventQueryService;
import com.citypulse.catalog.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "CityPulse's Events", description = "Endpoints for managing events")
@RequiredArgsConstructor
public class EventController {

    private final EventQueryService eventQueryService;
    private final FeedbackService feedbackService;

    @GetMapping("/events")
    @Operation(summary = "Find events", description = "Retrieve a list of events based on search criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = CursorPageResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public CursorPageResponse<EventSummaryResponse> findEvents(
            @Parameter(description = "Event search criteria") @Valid @ModelAttribute EventSearchRequest request
    ) {
        return eventQueryService.findEvents(request);
    }

    @GetMapping("/events/map")
    @Operation(summary = "Find map events", description = "Retrieve a list of events for mapping purposes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved map events", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = CursorPageResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public CursorPageResponse<EventMapMarkerResponse> findMapEvents(
            @Parameter(description = "Event search criteria") @Valid @ModelAttribute EventSearchRequest request
    ) {
        return eventQueryService.findMapEvents(request);
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Find event", description = "Retrieve details of a specific event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    })
    public EventDetailResponse findEvent(
            @Parameter(description = "The ID of the event to retrieve") @PathVariable String eventId
    ) {
        return eventQueryService.findById(eventId);
    }

    @GetMapping("/events/slug/{slug}")
    @Operation(summary = "Find event by slug", description = "Retrieve details of a specific event by its SEO slug")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    })
    public EventDetailResponse findEventBySlug(
            @Parameter(description = "The SEO slug of the event to retrieve") @PathVariable String slug
    ) {
        return eventQueryService.findBySlug(slug);
    }

    @PostMapping("/feedback")
    @Operation(summary = "Submit feedback", description = "Persist user feedback for Paname Spot")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback received", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    public SubmissionResponse submitFeedback(
            @Valid @RequestBody FeedbackSubmissionRequest request
    ) {
        return feedbackService.submitFeedback(request);
    }

    @PostMapping("/events/{slug}/reports")
    @Operation(summary = "Report event issue", description = "Persist a user report for an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report received", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = SubmissionResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    })
    public SubmissionResponse reportEvent(
            @PathVariable String slug,
            @Valid @RequestBody EventReportRequest request
    ) {
        return feedbackService.reportEvent(slug, request);
    }

    @GetMapping("/categories")
    @Operation(summary = "Find categories", description = "Retrieve a list of available event categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved categories", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = List.class))})
    })
    public List<String> findCategories() {
        return eventQueryService.findCategories();
    }
}
