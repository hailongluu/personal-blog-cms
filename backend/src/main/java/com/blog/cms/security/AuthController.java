package com.blog.cms.security;

import com.blog.cms.config.BlogProperties;
import com.blog.cms.security.dto.AuthResponse;
import com.blog.cms.security.dto.ChangePasswordRequest;
import com.blog.cms.security.dto.LoginRequest;
import com.blog.cms.security.dto.RefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Auth endpoints — /api/admin/auth/*
 *
 * <p>Public: login, refresh, csrf<br>
 * Protected: logout, me
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final BlogProperties props;
    private final CookieUtils cookieUtils;

    /**
     * POST /api/admin/auth/login — exchange email + password for cookies.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {

        String userAgent = request.getHeader("User-Agent");
        String ip = extractIp(request);

        AuthService.LoginResult result = authService.login(req, userAgent, ip);

        // Set cookies
        long accessTtl = props.getJwt().getAccessTokenTtlSeconds();
        long refreshTtl = props.getJwt().getRefreshTokenTtlSeconds();
        cookieUtils.addAccessCookie(response, result.accessToken(), accessTtl);
        cookieUtils.addRefreshCookie(response, result.refreshToken(), refreshTtl);

        // Generate CSRF token
        String csrfToken = UUID.randomUUID().toString();
        cookieUtils.addCsrfCookie(response, csrfToken, 86400);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("data", result.response());
        body.put("error", null);
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("csrfToken", csrfToken);
        body.put("meta", meta);

        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/admin/auth/refresh — exchange refresh token for new pair.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @Valid @RequestBody RefreshRequest req,
            HttpServletResponse response) {

        AuthService.LoginResult result = authService.refresh(req.refreshToken());

        long accessTtl = props.getJwt().getAccessTokenTtlSeconds();
        long refreshTtl = props.getJwt().getRefreshTokenTtlSeconds();
        cookieUtils.addAccessCookie(response, result.accessToken(), accessTtl);
        cookieUtils.addRefreshCookie(response, result.refreshToken(), refreshTtl);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("data", result.response());
        body.put("error", null);
        body.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/admin/auth/logout — revoke session, clear cookies.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @CookieValue(name = CookieUtils.REFRESH_COOKIE, required = false) String refreshToken,
            @AuthenticationPrincipal BlogUserDetails user,
            HttpServletResponse response) {

        if (refreshToken != null) {
            authService.logout(refreshToken);
        } else if (user != null) {
            authService.logoutAll(user.getId());
        }
        cookieUtils.clearAll(response);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("data", Map.of("message", "Logged out successfully"));
        body.put("error", null);
        body.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/admin/auth/me — current user info.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal BlogUserDetails user) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (user == null) {
            body.put("data", null);
            Map<String, Object> error = new java.util.LinkedHashMap<>();
            error.put("code", "UNAUTHENTICATED");
            error.put("message", "Not logged in");
            body.put("error", error);
            body.put("meta", Map.of("timestamp", Instant.now().toString()));
            return ResponseEntity.status(401).body(body);
        }
        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getUser().getDisplayName(),
                user.getRoleName(),
                null, null
        );
        body.put("data", response);
        body.put("error", null);
        body.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/admin/auth/csrf — return CSRF token (regenerates cookie).
     * Useful for SPA first-load.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, Object>> csrf(HttpServletResponse response) {
        String csrfToken = UUID.randomUUID().toString();
        cookieUtils.addCsrfCookie(response, csrfToken, 86400);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("data", Map.of("csrfToken", csrfToken, "headerName", props.getCsrf().getHeaderName()));
        body.put("error", null);
        body.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/admin/auth/change-password — change password for current user.
     * Body: { currentPassword, newPassword }
     * Invalidates all existing sessions.
     */
    @PostMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal BlogUserDetails user) {
        authService.changePassword(user.getId(), req.getCurrentPassword(), req.getNewPassword());

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("data", Map.of("message", "Password changed. Please log in again."));
        body.put("error", null);
        body.put("meta", Map.of("timestamp", Instant.now().toString()));
        return ResponseEntity.ok(body);
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
