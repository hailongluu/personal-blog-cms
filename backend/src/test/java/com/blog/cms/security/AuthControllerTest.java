package com.blog.cms.security;

import com.blog.cms.user.Role;
import com.blog.cms.user.RoleRepository;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController integration tests — full HTTP flow.
 * Covers: login success/fail, /me unauthenticated, /me authenticated, logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @BeforeEach
    void setUp() {
        // Delete in FK-safe order (skip seeded admin user)
        userRepository.findAll().forEach(u -> {
            if (!ADMIN_EMAIL.equals(u.getEmail())) {
                userRepository.delete(u);
            }
        });
        roleRepository.findAll().forEach(r -> {
            if (!"admin".equals(r.getName())) {
                roleRepository.delete(r);
            }
        });

        Role adminRole = roleRepository.findByName("admin").orElseGet(() ->
                roleRepository.save(Role.builder()
                        .name("admin")
                        .description("Admin")
                        .build()));

        userRepository.findByEmailWithRole(ADMIN_EMAIL).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(ADMIN_EMAIL)
                        .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                        .displayName("Admin")
                        .role(adminRole)
                        .isActive(true)
                        .build()));
    }

    @Test
    void login_validCredentials_returns200WithCookies() throws Exception {
        // Get CSRF cookie first
        MvcResult csrfResult = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfCookie = csrfResult.getResponse().getCookie("blog_csrf").getValue();

        String body = new ObjectMapper().writeValueAsString(Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD
        ));

        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("blog_csrf", csrfCookie))
                        .header("X-CSRF-Token", csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.role").value("admin"))
                .andExpect(cookie().exists("blog_access"))
                .andExpect(cookie().exists("blog_refresh"))
                .andExpect(cookie().exists("blog_csrf"))
                .andReturn();

        // Verify HttpOnly flag on access cookie
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        // Get CSRF cookie first
        MvcResult csrfResult = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfCookie = csrfResult.getResponse().getCookie("blog_csrf").getValue();

        String body = new ObjectMapper().writeValueAsString(Map.of(
                "email", ADMIN_EMAIL,
                "password", "wrong-password"
        ));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("blog_csrf", csrfCookie))
                        .header("X-CSRF-Token", csrfCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_validationError_returns400() throws Exception {
        // Get CSRF cookie first
        MvcResult csrfResult = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfCookie = csrfResult.getResponse().getCookie("blog_csrf").getValue();

        String body = new ObjectMapper().writeValueAsString(Map.of(
                "email", "not-an-email",
                "password", "short"
        ));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("blog_csrf", csrfCookie))
                        .header("X-CSRF-Token", csrfCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_authenticated_returns200() throws Exception {
        // Get CSRF cookie first
        MvcResult csrfResult = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfCookie = csrfResult.getResponse().getCookie("blog_csrf").getValue();

        // Login first
        String body = new ObjectMapper().writeValueAsString(Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("blog_csrf", csrfCookie))
                        .header("X-CSRF-Token", csrfCookie))
                .andExpect(status().isOk())
                .andReturn();

        String accessCookie = loginResult.getResponse().getCookie("blog_access").getValue();
        String newCsrfCookie = loginResult.getResponse().getCookie("blog_csrf").getValue();
        assertNotNull(accessCookie);

        // Use access cookie to call /me
        mockMvc.perform(get("/api/admin/auth/me")
                        .cookie(
                                new jakarta.servlet.http.Cookie("blog_access", accessCookie),
                                new jakarta.servlet.http.Cookie("blog_csrf", newCsrfCookie)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test
    void logout_clearsCookies() throws Exception {
        // Get CSRF cookie first
        MvcResult csrfResult = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfCookie = csrfResult.getResponse().getCookie("blog_csrf").getValue();

        // Login first
        String body = new ObjectMapper().writeValueAsString(Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("blog_csrf", csrfCookie))
                        .header("X-CSRF-Token", csrfCookie))
                .andExpect(status().isOk())
                .andReturn();

        String accessCookie = loginResult.getResponse().getCookie("blog_access").getValue();
        String refreshCookie = loginResult.getResponse().getCookie("blog_refresh").getValue();
        String newCsrfCookie = loginResult.getResponse().getCookie("blog_csrf").getValue();

        mockMvc.perform(post("/api/admin/auth/logout")
                        .cookie(
                                new jakarta.servlet.http.Cookie("blog_access", accessCookie),
                                new jakarta.servlet.http.Cookie("blog_refresh", refreshCookie),
                                new jakarta.servlet.http.Cookie("blog_csrf", newCsrfCookie)
                        )
                        .header("X-CSRF-Token", newCsrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Logged out successfully"));
    }
}
