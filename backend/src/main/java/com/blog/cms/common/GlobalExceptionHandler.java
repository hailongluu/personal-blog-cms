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

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", "VALIDATION_ERROR");
        error.put("message", "Request validation failed");
        error.put("details", fieldErrors);
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", "BAD_REQUEST");
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Bad request");
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(com.blog.cms.security.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(com.blog.cms.security.InvalidCredentialsException ex) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", "INVALID_CREDENTIALS");
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Invalid credentials");
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", "ACCESS_DENIED");
        error.put("message", "You don't have permission to access this resource");
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", "INTERNAL_ERROR");
        error.put("message", "An unexpected error occurred");
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
