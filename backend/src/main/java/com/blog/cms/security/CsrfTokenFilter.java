package com.blog.cms.security;

import com.blog.cms.config.BlogProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Custom CSRF filter — generates a token, sets it as a readable cookie,
 * and validates it on state-changing requests (POST/PUT/PATCH/DELETE).
 *
 * <p>The token is exposed via cookie + the {@code X-CSRF-Token} header.
 * Frontend reads cookie → echoes in header on POSTs. Backend compares them.
 *
 * <p>Built on Spring Security's {@link CsrfToken} but with custom logic:
 * - GET/HEAD/OPTIONS: skip CSRF check
 * - Otherwise: token must match between cookie + header
 */
@Component
@RequiredArgsConstructor
public class CsrfTokenFilter extends OncePerRequestFilter {

    private final BlogProperties props;
    private final CookieUtils cookieUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!props.getCsrf().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerName = props.getCsrf().getHeaderName();
        String cookieToken = cookieUtils.getCookieValue(request, CookieUtils.CSRF_COOKIE);
        String headerToken = request.getHeader(headerName);

        // 1. Read methods — skip CSRF, ensure cookie exists
        String method = request.getMethod();
        boolean safeMethod = "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);

        if (safeMethod) {
            if (cookieToken == null || cookieToken.isBlank()) {
                String newToken = UUID.randomUUID().toString();
                cookieUtils.addCsrfCookie(response, newToken, 86400); // 24h
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Mutating methods — require CSRF match
        // Skip CSRF for auth endpoints (login/refresh/logout) since user isn't authenticated yet
        // Skip CSRF for public API endpoints (newsletter signup, etc.)
        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.startsWith("/api/admin/auth/login")
                || path.startsWith("/api/admin/auth/refresh")
                || path.startsWith("/api/admin/auth/forgot-password")
                || path.startsWith("/api/admin/auth/reset-password");
        boolean isPublicEndpoint = path.startsWith("/api/public/");

        if (isAuthEndpoint || isPublicEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        if (cookieToken == null || cookieToken.isBlank()
                || headerToken == null || headerToken.isBlank()
                || !cookieToken.equals(headerToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"data":null,"error":{"code":"CSRF_TOKEN_INVALID","message":"CSRF token missing or mismatch"},"meta":{"timestamp":"%s"}}
                    """.formatted(java.time.Instant.now()));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
