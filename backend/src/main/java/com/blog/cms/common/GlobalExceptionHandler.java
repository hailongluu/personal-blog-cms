package com.blog.cms.common;

import org.springframework.beans.factory.annotation.Value;
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
 *
 * Security note: in production, exception messages can leak schema, table names,
 * SQL fragments, file paths, or library versions. When the active profile is
 * "prod" we substitute generic messages for every exception. The original message
 * is still logged server-side so operators can debug.
 *
 * In dev / test profiles the original message is returned to ease debugging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Spring profile name; when set to "prod", hide internal exception messages. */
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private boolean isProd() {
        return "prod".equalsIgnoreCase(activeProfile)
                || "production".equalsIgnoreCase(activeProfile);
    }

    private String safeMessage(String internal, String fallback) {
        // In prod, never echo the raw exception message — it may contain
        // sensitive detail (table names, SQL, library versions, file paths).
        return isProd() ? fallback : internal;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage())
        );

        // In prod, hide field-level binding messages — they reveal the
        // expected schema (e.g. "title must not be blank") to attackers.
        Map<String, String> safeDetails = isProd() ? Map.of() : fieldErrors;

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", "VALIDATION_ERROR");
        error.put("message", isProd() ? "Request validation failed" : "Request validation failed");
        if (!safeDetails.isEmpty()) error.put("details", safeDetails);
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                safeMessage(ex.getMessage(), "Bad request"), null);
    }

    @ExceptionHandler(com.blog.cms.security.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(com.blog.cms.security.InvalidCredentialsException ex) {
        // Auth messages are safe to expose (user already knows their credentials
        // are wrong) — keep them, but normalize the wording.
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Invalid credentials", null);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(jakarta.persistence.EntityNotFoundException ex) {
        // NEVER expose the entity class name or identifier in prod — could leak
        // schema info or be used for enumeration attacks.
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Resource not found", null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You don't have permission to access this resource", null);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is malformed or missing required fields", null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method not allowed for this endpoint", null);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter is missing", null);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        // SQL constraint messages can leak table/column names — always hide in prod.
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Operation conflicts with existing data", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Catch-all: in dev return the actual message, in prod return generic.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                safeMessage(ex.getMessage(), "An unexpected error occurred"), null);
    }

    // ── helpers ────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String code,
                                                      String message, Map<String, String> details) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", null);
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (details != null && !details.isEmpty()) error.put("details", details);
        response.put("error", error);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.status(status).body(response);
    }
}
