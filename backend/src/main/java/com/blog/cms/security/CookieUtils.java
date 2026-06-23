package com.blog.cms.security;

import com.blog.cms.config.BlogProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Build Secure HttpOnly cookies for access + refresh tokens.
 *
 * <p>Why ResponseCookie (not javax.servlet.http.Cookie)?<br>
 * ResponseCookie supports {@code SameSite} attribute which plain Servlet Cookie doesn't.
 */
@Component
public class CookieUtils {

    public static final String ACCESS_COOKIE = "blog_access";
    public static final String REFRESH_COOKIE = "blog_refresh";
    public static final String CSRF_COOKIE = "blog_csrf";

    private final BlogProperties props;

    public CookieUtils(BlogProperties props) {
        this.props = props;
    }

    /**
     * Build an HttpOnly cookie.
     */
    public ResponseCookie.ResponseCookieBuilder builder(String name, long maxAgeSeconds) {
        BlogProperties.Cookie cfg = props.getCookie();
        return ResponseCookie.from(name, "")
                .httpOnly(cfg.isHttpOnly())
                .secure(cfg.isSecure())
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cfg.getSameSite())
                .domain(cfg.getDomain().isEmpty() ? null : cfg.getDomain());
    }

    public void addAccessCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = builder(ACCESS_COOKIE, maxAgeSeconds)
                .value(token)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = builder(REFRESH_COOKIE, maxAgeSeconds)
                .value(token)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addCsrfCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        // CSRF cookie MUST be readable by JavaScript → NOT HttpOnly.
        ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE, token)
                .httpOnly(false)
                .secure(props.getCookie().isSecure())
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(props.getCookie().getSameSite())
                .domain(props.getCookie().getDomain().isEmpty() ? null : props.getCookie().getDomain())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAll(HttpServletResponse response) {
        // Clear all 3 cookies with maxAge=0
        for (String name : new String[]{ACCESS_COOKIE, REFRESH_COOKIE, CSRF_COOKIE}) {
            ResponseCookie cookie = ResponseCookie.from(name, "")
                    .path("/")
                    .maxAge(0)
                    .httpOnly(!CSRF_COOKIE.equals(name))
                    .secure(props.getCookie().isSecure())
                    .sameSite(props.getCookie().getSameSite())
                    .domain(props.getCookie().getDomain().isEmpty() ? null : props.getCookie().getDomain())
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }

    public String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
