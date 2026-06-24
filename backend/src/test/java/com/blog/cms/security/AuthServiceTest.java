package com.blog.cms.security;

import com.blog.cms.security.dto.AuthResponse;
import com.blog.cms.security.dto.LoginRequest;
import com.blog.cms.user.PasswordResetTokenRepository;
import com.blog.cms.user.Role;
import com.blog.cms.user.RoleRepository;
import com.blog.cms.user.SessionRepository;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService unit tests — RED-GREEN-REFACTOR.
 * Covers: login success, invalid credentials, refresh, logout, logout-all.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.findAll().forEach(u -> {
            if (!"admin@example.com".equals(u.getEmail())) {
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
                        .description("Test admin")
                        .build()));

        testUser = userRepository.findByEmailWithRole("test@example.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("test@example.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .displayName("Test User")
                        .role(adminRole)
                        .isActive(true)
                        .build()));
    }

    @Test
    void login_validCredentials_returnsTokens() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult result = authService.login(req, "TestAgent", "127.0.0.1");

        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals("test@example.com", result.response().email());
        assertEquals("admin", result.response().role());
        assertNotNull(result.response().accessExpiresAt());
        assertNotNull(result.response().refreshExpiresAt());

        // Verify JWT claims
        Claims claims = jwtService.parseAndVerify(result.accessToken());
        assertEquals(String.valueOf(testUser.getId()), claims.getSubject());
        assertEquals("admin", claims.get("role"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    void login_invalidPassword_throwsException() {
        LoginRequest req = new LoginRequest("test@example.com", "wrong-password");
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(req, "TestAgent", "127.0.0.1"));
    }

    @Test
    void login_unknownEmail_throwsException() {
        LoginRequest req = new LoginRequest("unknown@example.com", "password123");
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(req, "TestAgent", "127.0.0.1"));
    }

    @Test
    void login_inactiveUser_throwsException() {
        testUser.setIsActive(false);
        userRepository.save(testUser);

        LoginRequest req = new LoginRequest("test@example.com", "password123");
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(req, "TestAgent", "127.0.0.1"));
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult initial = authService.login(req, "TestAgent", "127.0.0.1");

        AuthService.LoginResult refreshed = authService.refresh(initial.refreshToken());

        assertNotNull(refreshed.accessToken());
        assertNotNull(refreshed.refreshToken());
        assertNotEquals(initial.refreshToken(), refreshed.refreshToken(),
                "Refresh token should rotate");
    }

    @Test
    void refresh_invalidToken_throwsException() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.refresh("not-a-real-token"));
    }

    @Test
    void logout_revokesSession() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult result = authService.login(req, "TestAgent", "127.0.0.1");

        authService.logout(result.refreshToken());

        // Try to refresh the revoked token — should fail
        assertThrows(InvalidCredentialsException.class,
                () -> authService.refresh(result.refreshToken()));
    }

    @Test
    void logoutAll_revokesAllUserSessions() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult r1 = authService.login(req, "Agent1", "1.1.1.1");
        AuthService.LoginResult r2 = authService.login(req, "Agent2", "2.2.2.2");

        authService.logoutAll(testUser.getId());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.refresh(r1.refreshToken()));
        assertThrows(InvalidCredentialsException.class,
                () -> authService.refresh(r2.refreshToken()));
    }

    @Test
    void changePassword_validCurrentPassword_succeedsAndRevokesSessions() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult r1 = authService.login(req, "Agent1", "1.1.1.1");

        authService.changePassword(testUser.getId(), "password123", "newSecret99");

        // Old sessions should be revoked
        assertThrows(InvalidCredentialsException.class,
                () -> authService.refresh(r1.refreshToken()));

        // New login should work with the new password
        LoginRequest newReq = new LoginRequest("test@example.com", "newSecret99");
        AuthService.LoginResult r2 = authService.login(newReq, "Agent", "3.3.3.3");
        assertThat(r2.accessToken()).isNotBlank();
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.changePassword(testUser.getId(), "wrong-password", "newSecret99"));
    }

    @Test
    void changePassword_tooShort_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword(testUser.getId(), "password123", "short"));
    }

    @Test
    void changePassword_sameAsCurrent_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword(testUser.getId(), "password123", "password123"));
    }

    // ═════════════════════════════════════════════════════════════════
    // Forgot Password / Reset Password — SPEC §7
    // ═════════════════════════════════════════════════════════════════

    @Test
    void forgotPassword_unknownEmail_doesNotThrow_andReturnsNull() {
        // Security: do not leak whether email exists
        // Should silently succeed (return null) for non-existent email
        String token = authService.requestPasswordReset("ghost@example.com");
        assertThat(token).isNull();
    }

    @Test
    void forgotPassword_existingEmail_returnsToken() {
        String token = authService.requestPasswordReset("test@example.com");
        assertThat(token).isNotBlank();
        assertThat(token.length()).isGreaterThanOrEqualTo(32);
    }

    @Test
    void forgotPassword_invalidEmail_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.requestPasswordReset("not-an-email"));
    }

    @Test
    void resetPassword_validToken_changesPassword_andRevokesSessions() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult r1 = authService.login(req, "Agent", "1.1.1.1");

        String token = authService.requestPasswordReset("test@example.com");
        assertThat(token).isNotBlank();

        authService.resetPassword(token, "newResetPwd!");

        // Old session should be revoked
        assertThrows(InvalidCredentialsException.class,
                () -> authService.refresh(r1.refreshToken()));

        // Login with new password must succeed
        LoginRequest newReq = new LoginRequest("test@example.com", "newResetPwd!");
        AuthService.LoginResult r2 = authService.login(newReq, "Agent", "2.2.2.2");
        assertThat(r2.accessToken()).isNotBlank();
    }

    @Test
    void resetPassword_invalidToken_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.resetPassword("bogus-token-value", "anyNewPwd1"));
    }

    @Test
    void resetPassword_tooShort_throws_andKeepsOldPassword() {
        String token = authService.requestPasswordReset("test@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword(token, "short"));

        // Old password must still work
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        AuthService.LoginResult result = authService.login(req, "Agent", "1.1.1.1");
        assertThat(result.accessToken()).isNotBlank();
    }

    @Test
    void resetPassword_tokenCanBeUsedOnlyOnce() {
        String token = authService.requestPasswordReset("test@example.com");
        authService.resetPassword(token, "firstNewPwd1");

        assertThrows(InvalidCredentialsException.class,
                () -> authService.resetPassword(token, "secondNewPwd1"));
    }
}
