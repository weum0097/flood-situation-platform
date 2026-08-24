package com.example.flood.common.api;

import com.example.flood.common.time.BeijingTime;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");
    private final Clock clock;
    private final PublicIdGenerator idGenerator;

    public GlobalExceptionHandler(Clock clock, PublicIdGenerator idGenerator) {
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return response(exception.errorCode(), exception.getMessage(), exception.details());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleMalformedRequest(HttpMessageNotReadableException exception) {
        return response(ErrorCode.MALFORMED_REQUEST, "Request body could not be parsed", List.of());
    }

    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> handleRequestBinding(Exception exception) {
        return response(ErrorCode.MALFORMED_REQUEST,
            "Required request values are missing or invalid", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiFieldError> details = exception.getBindingResult().getFieldErrors().stream()
            .sorted(java.util.Comparator.comparing(FieldError::getField))
            .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
            .toList();
        return response(ErrorCode.VALIDATION_ERROR, "Request validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiFieldError> details = exception.getConstraintViolations().stream()
            .map(violation -> new ApiFieldError(
                violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
        return response(ErrorCode.VALIDATION_ERROR, "Request validation failed", details);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected request failure", exception);
        return response(ErrorCode.INTERNAL_ERROR, "Internal server error", List.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
        ErrorCode errorCode,
        String message,
        List<ApiFieldError> details
    ) {
        String requestId = RequestContextHolder.current()
            .map(RequestContext::requestId)
            .orElseGet(() -> idGenerator.next("req_"));
        ApiErrorResponse body = new ApiErrorResponse(
            requestId,
            errorCode.name(),
            message,
            details,
            BeijingTime.now(clock));
        return ResponseEntity.status(errorCode.status()).contentType(PROBLEM_JSON).body(body);
    }
}
