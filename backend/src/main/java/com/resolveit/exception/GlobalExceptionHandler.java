package com.resolveit.exception;

import com.resolveit.web.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** One place that turns every failure into a uniform ApiError. Stack traces never leave the server. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ItsmException.class)
    public ResponseEntity<ApiError> domain(ItsmException e, HttpServletRequest request) {
        Map<String, String> fields = e instanceof ValidationException v ? v.getFieldErrors() : null;
        Map<String, Object> details = e.getDetails().isEmpty() ? null : e.getDetails();
        return body(e.getHttpStatus(), e.getErrorCode(), e.getUserMessage(), request, fields, details, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> fields.putIfAbsent(err.getField(), err.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Please correct the highlighted fields.", request, fields, null, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> unreadable(Exception e, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "The request is missing data or is malformed.", request, null, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest request) {
        return body(HttpStatus.FORBIDDEN, "UNAUTHORIZED_OPERATION", "You do not have access to this resource.", request, null, null, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> stale(OptimisticLockingFailureException e, HttpServletRequest request) {
        return body(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "Someone else updated this incident. Please refresh and try again.", request, null, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        return body(HttpStatus.CONFLICT, "CONFLICT", "This change conflicts with existing data.", request, null, null, null);
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> database(Exception e, HttpServletRequest request) {
        String id = UUID.randomUUID().toString();
        log.error("Database unavailable [{}]: {}", id, e.getMessage());
        return body(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE", "Database temporarily unavailable. Please try again shortly.", request, null, null, id);
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiError> notFound(Exception e, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "This endpoint does not exist.", request, null, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        String id = UUID.randomUUID().toString();
        log.error("Unexpected failure [{}] on {}", id, request.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be completed.", request, null, null, id);
    }

    private static ResponseEntity<ApiError> body(HttpStatus status, String code, String message, HttpServletRequest request,
                                                 Map<String, String> fields, Map<String, Object> details, String correlationId) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message,
                request == null ? null : request.getRequestURI(), fields == null || fields.isEmpty() ? null : fields, details, correlationId));
    }
}
