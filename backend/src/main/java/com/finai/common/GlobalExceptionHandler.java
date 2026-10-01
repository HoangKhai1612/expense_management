package com.finai.common;

import jakarta.servlet.http.HttpServletRequest;
import com.finai.admin.SystemMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Maps every failure mode to {@link ApiError} so clients can rely on one shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Optional; absent in slice tests that do not build the full context. */
    private final ObjectProvider<SystemMetricsService> systemMetricsService;

    public GlobalExceptionHandler(ObjectProvider<SystemMetricsService> systemMetricsService) {
        this.systemMetricsService = systemMetricsService;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiError.of(ex.getStatus().value(), ex.getCode(), ex.getMessage(), path(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return ResponseEntity.badRequest().body(ApiError.of(400, "VALIDATION_ERROR",
                "One or more fields are invalid.", path(request), violations));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "MALFORMED_REQUEST",
                "Request body is missing or cannot be parsed.", path(request)));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadParameter(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "INVALID_PARAMETER",
                "A request parameter is missing or has the wrong type.", path(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex,
                                                    HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", path(request), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, "CONSTRAINT_VIOLATION",
                "The operation conflicts with existing data.", path(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(403, "FORBIDDEN",
                "You do not have permission to perform this action.", path(request)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(401, "UNAUTHORIZED",
                "Authentication is required.", path(request)));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex,
                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(404, "NOT_FOUND",
                "No endpoint matches this request.", path(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // The full cause is logged, never returned: production clients must not see internals.
        log.error("Unhandled exception on {} {}", request.getMethod(), path(request), ex);
        SystemMetricsService metrics = systemMetricsService.getIfAvailable();
        if (metrics != null) {
            metrics.recordUnhandledError();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(500,
                "INTERNAL_ERROR", "The server could not complete the request.", path(request)));
    }

    private ApiError.FieldViolation toViolation(FieldError error) {
        return new ApiError.FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
