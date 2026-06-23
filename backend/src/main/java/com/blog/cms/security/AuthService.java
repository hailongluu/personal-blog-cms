package com.blog.cms.security;

import com.blog.cms.config.BlogProperties;
import com.blog.cms.security.dto.AuthResponse;
import com.blog.cms.security.dto.LoginRequest;
import com.blog.cms.user.Session;
import com.blog.cms.user.SessionRepository;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Auth business logic — login, refresh, logout, current user.
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
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

    public record LoginResult(String accessToken, String refreshToken, AuthResponse response) {}
}
