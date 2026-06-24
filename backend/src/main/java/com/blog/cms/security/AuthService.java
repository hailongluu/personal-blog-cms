package com.blog.cms.security;

import com.blog.cms.config.BlogProperties;
import com.blog.cms.security.dto.AuthResponse;
import com.blog.cms.security.dto.LoginRequest;
import com.blog.cms.user.PasswordResetToken;
import com.blog.cms.user.PasswordResetTokenRepository;
import com.blog.cms.user.Session;
import com.blog.cms.user.SessionRepository;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Auth business logic — login, refresh, logout, current user, password reset.
 *
 * <p>Login flow:
 * <ol>
 *   <li>Find user by email</li>
 *   <li>Verify password (BCrypt)</li>
 *   <li>Check user is active and not deleted</li>
 *   <li>Issue access JWT + refresh opaque token</li>
 *   <li>Persist session (refresh token + UA + IP)</li>
 *   <li>Update last_login_at</li>
 * </ol>
 *
 * <p>Password reset flow (SPEC §7):
 * <ol>
 *   <li>requestPasswordReset(email): generate random token, hash + persist with TTL, return raw token</li>
 *   <li>resetPassword(token, newPassword): look up by hash, validate not expired/used, update password, mark used, revoke all sessions</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RESET_TTL_SECONDS = 3600;  // 1 hour

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BlogProperties props;

    @Transactional
    public LoginResult login(LoginRequest req, String userAgent, String ipAddress) {
        User user = userRepository.findByEmailWithRole(req.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getDeletedAt() != null) {
            throw new InvalidCredentialsException("Account is inactive");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email={} from ip={}", req.email(), ipAddress);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Issue tokens
        String accessToken = jwtService.issueAccessToken(
                user.getId(), user.getEmail(), user.getRole().getName());
        String refreshToken = jwtService.issueRefreshToken();

        // Persist session
        Session session = Session.builder()
                .user(user)
                .refreshToken(refreshToken)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();
        sessionRepository.save(session);

        // Update last login
        userRepository.updateLastLoginAt(user.getId(), Instant.now());

        long accessTtl = props.getJwt().getAccessTokenTtlSeconds();
        long refreshTtl = props.getJwt().getRefreshTokenTtlSeconds();

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().getName(),
                Instant.now().plusSeconds(accessTtl).getEpochSecond(),
                Instant.now().plusSeconds(refreshTtl).getEpochSecond()
        );

        return new LoginResult(accessToken, refreshToken, response);
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (!session.isActive()) {
            throw new InvalidCredentialsException("Refresh token expired or revoked");
        }

        User user = session.getUser();
        // Rotate refresh token (defense in depth)
        String newRefresh = jwtService.issueRefreshToken();
        session.setRefreshToken(newRefresh);
        session.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()));
        sessionRepository.save(session);

        String accessToken = jwtService.issueAccessToken(
                user.getId(), user.getEmail(), user.getRole().getName());

        long accessTtl = props.getJwt().getAccessTokenTtlSeconds();
        long refreshTtl = props.getJwt().getRefreshTokenTtlSeconds();

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().getName(),
                Instant.now().plusSeconds(accessTtl).getEpochSecond(),
                Instant.now().plusSeconds(refreshTtl).getEpochSecond()
        );

        return new LoginResult(accessToken, newRefresh, response);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRepository.revokeByToken(refreshToken, Instant.now());
        }
    }

    @Transactional
    public void logoutAll(Long userId) {
        sessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    /**
     * Change password for authenticated user — SPEC §7.6.
     * Verifies current password, rejects if too weak, invalidates all sessions.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // SPEC §7.6: min length 8, must differ from current
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all sessions on password change (defense in depth)
        sessionRepository.revokeAllByUserId(userId, Instant.now());
        log.info("Password changed for user id={}, all sessions revoked", userId);
    }

    public record LoginResult(String accessToken, String refreshToken, AuthResponse response) {}

    // ═════════════════════════════════════════════════════════════════
    // Password reset — SPEC §7
    // ═════════════════════════════════════════════════════════════════

    /**
     * Request a password reset token for the given email.
     *
     * <p>Security: silently returns {@code null} if the email does not exist
     * to prevent user enumeration. Only existing users receive a token.
     * Token is returned in plain (to be sent via email); only its SHA-256
     * hash is persisted.
     *
     * @param email user email
     * @return raw token (32 random bytes, base64url-encoded), or null if email not found
     * @throws IllegalArgumentException if email format is invalid
     */
    @Transactional
    public String requestPasswordReset(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String normalized = email.trim().toLowerCase();
        var userOpt = userRepository.findByEmailWithRole(normalized);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email (silent no-op)");
            return null;
        }

        User user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getDeletedAt() != null) {
            return null;
        }

        // Generate random token (32 bytes -> 43-char base64url)
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = sha256Hex(rawToken);

        // Persist hashed token with TTL
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(RESET_TTL_SECONDS))
                .build();
        passwordResetTokenRepository.save(token);

        log.info("Password reset token issued for user id={} (email={})", user.getId(), normalized);
        return rawToken;
    }

    /**
     * Reset password using a previously issued token.
     *
     * @param rawToken the plain token from the email
     * @param newPassword new password (min 8 chars per SPEC §7.6)
     * @throws InvalidCredentialsException if token not found, expired, or already used
     * @throws IllegalArgumentException if newPassword is too short
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException("Invalid reset token");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        String tokenHash = sha256Hex(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token"));

        if (!token.isValid()) {
            throw new InvalidCredentialsException("Reset token expired or already used");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        // Revoke all sessions (defense in depth — see changePassword)
        sessionRepository.revokeAllByUserId(user.getId(), Instant.now());

        log.info("Password reset successfully for user id={}, all sessions revoked", user.getId());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
