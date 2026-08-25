package com.citypulse.catalog.controller;

import com.citypulse.catalog.config.AdminTokenGuard;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventReportResponse;
import com.citypulse.catalog.dto.response.FeedbackSubmissionResponse;
import com.citypulse.catalog.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only admin endpoints for reviewing user submissions. Guarded by a shared
 * secret supplied in the {@code X-Admin-Token} header; these expose emails and
 * so must never be reachable without it.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Admin - Submissions", description = "Read received feedback and event reports")
@RequiredArgsConstructor
public class FeedbackAdminController {

    static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final FeedbackService feedbackService;
    private final AdminTokenGuard adminTokenGuard;

    @GetMapping("/feedback")
    @Operation(summary = "List feedback", description = "Retrieve received feedback, newest first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved feedback", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = CursorPageResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid admin token", content = @Content)
    })
    public CursorPageResponse<FeedbackSubmissionResponse> listFeedback(
            @RequestHeader(value = ADMIN_TOKEN_HEADER, required = false) String token,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (max 100)") @RequestParam(required = false) Integer size
    ) {
        adminTokenGuard.requireValidToken(token);

        return feedbackService.listFeedback(page, size);
    }

    @GetMapping("/reports")
    @Operation(summary = "List event reports", description = "Retrieve received event reports, newest first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reports", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = CursorPageResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Missing or invalid admin token", content = @Content)
    })
    public CursorPageResponse<EventReportResponse> listReports(
            @RequestHeader(value = ADMIN_TOKEN_HEADER, required = false) String token,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (max 100)") @RequestParam(required = false) Integer size
    ) {
        adminTokenGuard.requireValidToken(token);

        return feedbackService.listReports(page, size);
    }
}
