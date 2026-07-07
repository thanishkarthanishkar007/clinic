package com.devsclinic.backend.exception;

import com.devsclinic.backend.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handling for all REST controllers.
 * Ensures the frontend always receives a consistent, predictable error shape,
 * and that internal stack traces / details are never leaked to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Triggered when @Valid validation fails on a request body (e.g. AppointmentRequest).
     * Returns field-level error messages so the frontend can highlight exact fields.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed. Please check the highlighted fields.")
                .data(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Triggered for constraint violations outside of @RequestBody validation
     * (e.g. @RequestParam / @PathVariable validation).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Resource (e.g. appointment) not found by id.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Email delivery failure. Returned as 502 (Bad Gateway) since the
     * failure originates from an upstream dependency (SMTP server),
     * not the client's request. The appointment itself is still saved.
     */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailSendException(EmailSendException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("Appointment was saved, but notification email could not be sent: "
                        + ex.getMessage()));
    }

    /**
     * Catch-all fallback for any unhandled exception.
     * Never exposes internal stack traces to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
