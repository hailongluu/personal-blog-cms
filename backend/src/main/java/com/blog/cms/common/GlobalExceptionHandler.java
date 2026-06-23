package com.blog.cms.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler — converts exceptions to standard API response format.
 * Format: { data, error: { code, message, details? }, meta }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage())
        );

        Map<String, Object> response = Map.of(
                "data", null,
                "error", Map.of(
                        "code", "VALIDATION_ERROR",
                        "message", "Request validation failed",
                        "details", fieldErrors
                ),
                "meta", Map.of("timestamp", Instant.now().toString())
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> response = Map.of(
                "data", null,
                "error", Map.of(
                        "code", "BAD_REQUEST",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Bad request"
                ),
                "meta", Map.of("timestamp", Instant.now().toString())
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> response = Map.of(
                "data", null,
                "error", Map.of(
                        "code", "INTERNAL_ERROR",
                        "message", "An unexpected error occurred"
                ),
                "meta", Map.of("timestamp", Instant.now().toString())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
