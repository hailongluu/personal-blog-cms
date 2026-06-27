package com.blog.cms.common;

import com.blog.cms.security.InvalidCredentialsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that GlobalExceptionHandler hides internal exception details when
 * running under the "prod" profile, while preserving them in dev for debugging.
 *
 * Security property: production responses must NEVER include raw exception
 * messages, stack traces, or schema details that an attacker could use to
 * map the backend.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    private void setProfile(String profile) {
        ReflectionTestUtils.setField(handler, "activeProfile", profile);
    }

    @Test
    @DisplayName("prod: EntityNotFoundException returns generic message, no internal text")
    void prod_notFound_hidesInternalMessage() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleNotFound(
                new EntityNotFoundException("Post with slug=secret-internal-id not found in posts table"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("NOT_FOUND");
        assertThat(error.get("message")).isEqualTo("Resource not found");
        // Critical: raw exception message must NOT leak in prod
        assertThat(error.get("message").toString())
                .doesNotContain("secret-internal-id", "posts table", "slug");
    }

    @Test
    @DisplayName("dev: EntityNotFoundException returns generic message (always — never leak table/ID)")
    void dev_notFound_returnsGenericMessage() {
        // EntityNotFoundException messages typically contain class names + IDs
        // (e.g. "Post#42 not found"), which leak schema info. We strip them
        // in BOTH dev and prod. The exception is still logged server-side.
        setProfile("dev");
        ResponseEntity<Map<String, Object>> res = handler.handleNotFound(
                new EntityNotFoundException("Post with slug=secret-internal-id not found in posts table"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("NOT_FOUND");
        assertThat(error.get("message")).isEqualTo("Resource not found");
        assertThat(error.get("message").toString())
                .doesNotContain("secret-internal-id", "posts table", "slug");
    }

    @Test
    @DisplayName("prod: DataIntegrityViolationException hides SQL constraint details")
    void prod_dataIntegrity_hidesSchemaDetails() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleDataIntegrity(
                new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"users_email_key\""));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("DATA_CONFLICT");
        assertThat(error.get("message")).isEqualTo("Operation conflicts with existing data");
        assertThat(error.get("message").toString())
                .doesNotContain("users_email_key", "duplicate", "constraint");
    }

    @Test
    @DisplayName("prod: HttpMessageNotReadableException hides parser internals")
    void prod_malformedRequest_hidesJsonPath() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleUnreadable(
                new HttpMessageNotReadableException("JSON parse error: Unexpected character at line 5"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("MALFORMED_REQUEST");
        assertThat(error.get("message").toString()).doesNotContain("line 5", "JSON parse");
    }

    @Test
    @DisplayName("prod: HttpRequestMethodNotSupportedException returns generic message")
    void prod_methodNotAllowed_hidesMethod() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("DELETE",
                        java.util.List.of("GET", "POST")));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(error.get("message").toString()).doesNotContain("DELETE", "GET");
    }

    @Test
    @DisplayName("prod: MissingServletRequestParameterException hides parameter name")
    void prod_missingParam_hidesParameterName() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleMissingParam(
                new MissingServletRequestParameterException("adminSecretToken", "String"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("MISSING_PARAMETER");
        assertThat(error.get("message").toString()).doesNotContain("adminSecretToken");
    }

    @Test
    @DisplayName("prod: InvalidCredentialsException returns safe standard message")
    void prod_invalidCredentials_returnsStandardMessage() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleAuth(
                new InvalidCredentialsException("user admin@secret.com attempted login from 10.0.0.1"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(error.get("message")).isEqualTo("Invalid credentials");
        // Email + IP from internal exception must NEVER leak
        assertThat(error.get("message").toString())
                .doesNotContain("admin@secret.com", "10.0.0.1");
    }

    @Test
    @DisplayName("prod: catch-all Exception returns generic message, no stack info")
    void prod_genericException_hidesInternalMessage() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleGeneric(
                new RuntimeException("NullPointerException at UserService.findById:42 — schema.users has no column email_verified"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("INTERNAL_ERROR");
        assertThat(error.get("message")).isEqualTo("An unexpected error occurred");
        assertThat(error.get("message").toString())
                .doesNotContain("NullPointerException", "UserService", "schema.users", "email_verified", "line 42");
    }

    @Test
    @DisplayName("dev: catch-all Exception preserves actual message for debugging")
    void dev_genericException_keepsMessage() {
        setProfile("dev");
        ResponseEntity<Map<String, Object>> res = handler.handleGeneric(
                new RuntimeException("Detailed error: connection refused to db:5432"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("message")).isEqualTo("Detailed error: connection refused to db:5432");
    }

    @Test
    @DisplayName("default profile (no active): EntityNotFoundException still returns generic (not exception-internal)")
    void defaultProfile_notFound_returnsGenericMessage() {
        // EntityNotFoundException always returns generic — it can leak IDs/schema
        // regardless of profile. The profile check only affects messages for
        // IllegalArgumentException, DataIntegrityViolation, and the catch-all.
        setProfile("default");
        ResponseEntity<Map<String, Object>> res = handler.handleNotFound(
                new EntityNotFoundException("Post with id=42 not found"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("message")).isEqualTo("Resource not found");
        assertThat(error.get("message").toString()).doesNotContain("id=42");
    }

    @Test
    @DisplayName("prod IllegalArgumentException hides internal message")
    void prod_illegalArgument_hidesMessage() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleBadRequest(
                new IllegalArgumentException("Invalid admin secret key length: expected 64 chars"));

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("BAD_REQUEST");
        assertThat(error.get("message")).isEqualTo("Bad request");
        assertThat(error.get("message").toString()).doesNotContain("admin secret", "64 chars");
    }

    @Test
    @DisplayName("prod response has standard envelope: { data, error: { code, message }, meta }")
    void prod_responseEnvelope_isConsistent() {
        setProfile("prod");
        ResponseEntity<Map<String, Object>> res = handler.handleNotFound(
                new EntityNotFoundException("anything"));

        Map<String, Object> body = res.getBody();
        assertThat(body).containsKeys("data", "error", "meta");
        assertThat(body.get("data")).isNull();
        assertThat(body.get("error")).isInstanceOf(Map.class);
        assertThat(body.get("meta")).isInstanceOf(Map.class);
        Map<String, Object> meta = (Map<String, Object>) body.get("meta");
        assertThat(meta).containsKey("timestamp");
    }

    @Test
    @DisplayName("prod MethodArgumentNotValidException hides field details")
    void prod_validation_hidesFieldNames() throws Exception {
        setProfile("prod");
        // Build a fake MethodArgumentNotValidException by invoking the handler
        // directly with a stubbed binding result. The simplest reliable path is
        // to call the handler with a real exception built from a BindingResult.
        org.springframework.validation.BeanPropertyBindingResult br =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "obj");
        br.addError(new org.springframework.validation.FieldError(
                "obj", "adminSecretField", "must not be blank"));
        org.springframework.core.MethodParameter mp = null; // not used by handler
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

        ResponseEntity<Map<String, Object>> res = handler.handleValidation(ex);

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error.get("code")).isEqualTo("VALIDATION_ERROR");
        assertThat(error.get("message")).isEqualTo("Request validation failed");
        // Prod must not leak field names or expected-value details
        assertThat(error).doesNotContainKey("details");
    }

    @Test
    @DisplayName("dev MethodArgumentNotValidException includes field details for debugging")
    void dev_validation_keepsFieldDetails() throws Exception {
        setProfile("dev");
        org.springframework.validation.BeanPropertyBindingResult br =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "obj");
        br.addError(new org.springframework.validation.FieldError(
                "obj", "title", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, br);

        ResponseEntity<Map<String, Object>> res = handler.handleValidation(ex);

        Map<String, Object> error = (Map<String, Object>) res.getBody().get("error");
        assertThat(error).containsKey("details");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) error.get("details");
        assertThat(details).containsKey("title");
        assertThat(details.get("title")).isEqualTo("must not be blank");
    }
}
