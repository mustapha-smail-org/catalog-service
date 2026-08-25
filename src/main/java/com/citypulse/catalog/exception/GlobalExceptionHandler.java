package com.citypulse.catalog.exception;

import com.citypulse.catalog.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ProblemDetail> handleEventNotFound(EventNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.EVENT_NOT_FOUND, "Event not found", exception.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "Unauthorized", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidCursorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ProblemDetail> handleInvalidCursor(InvalidCursorException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_CURSOR, "Invalid pagination cursor", exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        Map<String, Object> properties = Map.of("parameter", exception.getName(), "rejectedValue", String.valueOf(exception.getValue()));

        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_PARAMETER, "Invalid request parameter", "Parameter '%s' has an invalid value".formatted(exception.getName()), request, properties);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        List<Map<String, String>> violations = exception.getConstraintViolations().stream().map(violation -> Map.of("field", violation.getPropertyPath().toString(), "message", violation.getMessage())).toList();

        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Validation failed", "One or more request values are invalid", request, Map.of("violations", violations));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ResponseEntity<ProblemDetail> handleDataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Database integrity conflict path={}", request.getRequestURI(), exception);

        return problem(HttpStatus.CONFLICT, ApiErrorCode.DATA_CONFLICT, "Data conflict", "The operation conflicts with existing data", request, null);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ResponseEntity<ProblemDetail> handleDatabaseUnavailable(DataAccessResourceFailureException exception, HttpServletRequest request) {
        log.error("Database unavailable path={}", request.getRequestURI(), exception);

        return problem(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.SERVICE_UNAVAILABLE, "Service temporarily unavailable", "The service cannot complete the request at this time", request, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ResponseEntity<ProblemDetail> handleUnexpectedFailure(Exception exception, HttpServletRequest request) {
        log.error("Unexpected REST failure path={}", request.getRequestURI(), exception);

        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, "Internal server error", "An unexpected error occurred", request, null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, org.springframework.web.context.request.WebRequest webRequest) {
        HttpServletRequest request = ((org.springframework.web.context.request.ServletWebRequest) webRequest).getRequest();

        List<Map<String, String>> violations = exception.getBindingResult().getAllErrors().stream().map(error -> {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();

            return Map.of("field", field, "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
        }).toList();

        ProblemDetail body = createProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Validation failed", "One or more request values are invalid", request, Map.of("violations", violations));

        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, org.springframework.web.context.request.WebRequest webRequest) {
        HttpServletRequest request = ((org.springframework.web.context.request.ServletWebRequest) webRequest).getRequest();

        ProblemDetail body = createProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_PARAMETER, "Malformed request", "The request body could not be parsed", request, null);

        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, ApiErrorCode code, String title, String detail, HttpServletRequest request, Map<String, Object> additionalProperties) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(createProblem(status, code, title, detail, request, additionalProperties));
    }

    private ProblemDetail createProblem(HttpStatus status, ApiErrorCode code, String title, String detail, HttpServletRequest request, Map<String, Object> additionalProperties) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);
        problem.setType(URI.create("https://api.citypulse.dev/problems/" + code.name().toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now(clock));
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));

        if (additionalProperties != null) {
            additionalProperties.forEach(problem::setProperty);
        }

        return problem;
    }
}