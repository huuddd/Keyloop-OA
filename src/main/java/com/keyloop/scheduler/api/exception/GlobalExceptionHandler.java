package com.keyloop.scheduler.api.exception;

import com.keyloop.scheduler.application.EntityNotFoundException;
import com.keyloop.scheduler.application.validation.ValidationException;
import com.keyloop.scheduler.domain.shared.Error;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URI =
        "https://api.scheduler.keyloop.com/problems/";

    /**
     * Convert domain Error (from Result pattern) to ProblemDetail.
     */
    public static ProblemDetail fromDomainError(Error error, HttpStatus status,
                                                 String instance) {
        var problem = ProblemDetail.forStatusAndDetail(status, error.message());
        problem.setType(URI.create(PROBLEM_BASE_URI + toKebabCase(error.code())));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(instance));
        problem.setProperty("errorCode", error.code());
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /**
     * Map domain error code to HTTP status.
     */
    public static HttpStatus mapErrorToStatus(Error error) {
        var code = error.code();
        if (code.endsWith(".NotFound")) return HttpStatus.NOT_FOUND;
        if (code.contains("NotAvailable") || code.contains("Available")
                || code.contains("Conflict"))
            return HttpStatus.CONFLICT;
        if (code.contains("InvalidTransition"))
            return HttpStatus.UNPROCESSABLE_ENTITY;
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex,
                                         HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + "entity-not-found"));
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "entity-not-found");
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex,
                                                     HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "validation-error");
        problem.setProperty("violations", ex.getValidationResult().getErrors());
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Bad Request");

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(
                URI.create(servletRequest.getRequest().getRequestURI()));
        }

        List<Map<String, String>> violations = ex.getBindingResult()
            .getFieldErrors().stream()
            .map(fe -> Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage() != null
                    ? fe.getDefaultMessage() : "invalid"))
            .toList();

        problem.setProperty("errorCode", "validation-error");
        problem.setProperty("violations", violations);
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex,
                                           HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + "invalid-request"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "invalid-request");
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handleLockFailure(PessimisticLockingFailureException ex,
                                            HttpServletRequest request) {
        log.warn("Lock contention at {}: {}", request.getRequestURI(), ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The requested resource is currently being modified. Please retry.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "lock-contention"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "lock-contention");
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex,
                                        HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(),
            ex.getMessage(), ex);
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "internal-error");
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private static String toKebabCase(String input) {
        if (input == null || input.isBlank()) return "unknown";
        return input
            .replace('.', '-')
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase();
    }
}
