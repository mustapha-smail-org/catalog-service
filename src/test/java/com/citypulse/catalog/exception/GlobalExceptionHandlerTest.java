package com.citypulse.catalog.exception;

import com.citypulse.catalog.config.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-13T10:30:00Z");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/events/bad");

    @BeforeEach
    void setCorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "correlation-42");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldTranslateNotFound() {
        ProblemDetail problem = handler.handleEventNotFound(
                new EventNotFoundException("missing"), request
        ).getBody();

        assertProblem(problem, HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
        assertThat(problem.getDetail()).contains("missing");
    }

    @Test
    void shouldTranslateInvalidCursor() {
        ProblemDetail problem = handler.handleInvalidCursor(
                new InvalidCursorException(new IllegalArgumentException()), request
        ).getBody();

        assertProblem(problem, HttpStatus.BAD_REQUEST, "INVALID_CURSOR");
    }

    @Test
    void shouldTranslateTypeMismatchWithRejectedValue() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "bad", Integer.class, "limit", null, new NumberFormatException()
        );

        ProblemDetail problem = handler.handleTypeMismatch(exception, request).getBody();

        assertProblem(problem, HttpStatus.BAD_REQUEST, "INVALID_PARAMETER");
        assertThat(problem.getProperties()).containsEntry("parameter", "limit")
                .containsEntry("rejectedValue", "bad");
    }

    @Test
    void shouldTranslateConstraintViolations() {
        ProblemDetail problem = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of()), request
        ).getBody();

        assertProblem(problem, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        assertThat(problem.getProperties()).containsKey("violations");
    }

    @Test
    void shouldTranslateDatabaseConflictAndOutage() {
        ProblemDetail conflict = handler.handleDataConflict(
                new DataIntegrityViolationException("duplicate"), request
        ).getBody();
        ProblemDetail unavailable = handler.handleDatabaseUnavailable(
                new DataAccessResourceFailureException("offline"), request
        ).getBody();

        assertProblem(conflict, HttpStatus.CONFLICT, "DATA_CONFLICT");
        assertProblem(unavailable, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }

    @Test
    void shouldHideUnexpectedFailureDetails() {
        ProblemDetail problem = handler.handleUnexpectedFailure(
                new RuntimeException("secret internal detail"), request
        ).getBody();

        assertProblem(problem, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        assertThat(problem.getDetail()).doesNotContain("secret");
    }

    @Test
    void shouldTranslateInvalidArgumentsIncludingFieldAndObjectErrors() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "limit", null, false, null, null, "Too large"));
        binding.addError(new ObjectError("request", (String) null));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, binding);

        var response = handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(request)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertProblem(problem, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        assertThat(problem.getProperties().get("violations").toString())
                .contains("limit", "Too large", "Invalid value");
    }

    @Test
    void shouldTranslateMalformedRequestBody() {
        var response = handler.handleHttpMessageNotReadable(
                mock(HttpMessageNotReadableException.class),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(request)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertProblem((ProblemDetail) response.getBody(), HttpStatus.BAD_REQUEST, "INVALID_PARAMETER");
    }

    private void assertProblem(ProblemDetail problem, HttpStatus status, String code) {
        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getInstance().toString()).isEqualTo("/api/v1/events/bad");
        assertThat(problem.getProperties())
                .containsEntry("code", code)
                .containsEntry("timestamp", NOW)
                .containsEntry("correlationId", "correlation-42");
    }
}
