package com.blog.cms.security;

import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extract JWT from HttpOnly cookie → verify → load user → set SecurityContext.
 *
 * <p>If cookie missing or invalid, request proceeds unauthenticated
 * (downstream rules decide whether to allow it).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CookieUtils cookieUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = cookieUtils.getCookieValue(request, CookieUtils.ACCESS_COOKIE);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseAndVerify(token);
            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                throw new JwtException("Not an access token");
            }
            Long userId = Long.valueOf(claims.getSubject());

            User user = userRepository.findByIdWithRole(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive()) || user.getDeletedAt() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            BlogUserDetails details = new BlogUserDetails(user);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    details, null, details.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT auth failed: {}", e.getMessage());
            // Clear invalid token so client re-auths
            cookieUtils.clearAll(response);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
