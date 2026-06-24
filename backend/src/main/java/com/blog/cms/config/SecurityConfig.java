package com.blog.cms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.blog.cms.security.CookieUtils;
import com.blog.cms.security.CsrfTokenFilter;
import com.blog.cms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security configuration — Spring Security 6 filter chain.
 *
 * <p>Order of filters:
 * <ol>
 *   <li>{@link CsrfTokenFilter} — first, reject bad CSRF early</li>
 *   <li>{@link JwtAuthenticationFilter} — extract JWT from cookie, set SecurityContext</li>
 *   <li>Standard Spring filters (anonymous, exception translation, ...)</li>
 * </ol>
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>{@code /api/health/**}, {@code /actuator/health}, {@code /api/public/**} — public</li>
 *   <li>{@code /api/admin/auth/login}, {@code /refresh} — public (no JWT yet)</li>
 *   <li>{@code /api/admin/**} — authenticated (JWT in cookie)</li>
 *   <li>Role-specific endpoints enforced via {@code @PreAuthorize}</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfTokenFilter csrfTokenFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health/**",
                                "/api/public/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/uploads/**",
                                "/robots.txt",
                                "/sitemap.xml",
                                "/.well-known/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(
                                "/api/admin/auth/login",
                                "/api/admin/auth/refresh",
                                "/api/admin/auth/csrf",
                                "/api/admin/auth/forgot-password",
                                "/api/admin/auth/reset-password"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(csrfTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Return 401 JSON instead of default 403 HTML for unauthenticated requests.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("data", null);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", "UNAUTHENTICATED");
            error.put("message", "Authentication required");
            body.put("error", error);
            body.put("meta", Map.of("timestamp", Instant.now().toString()));
            mapper.writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("data", null);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", "ACCESS_DENIED");
            error.put("message", "You don't have permission to access this resource");
            body.put("error", error);
            body.put("meta", Map.of("timestamp", Instant.now().toString()));
            mapper.writeValue(response.getOutputStream(), body);
        };
    }
}
