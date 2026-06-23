package com.blog.cms.security;

import com.blog.cms.config.BlogProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT service — issue and verify HMAC-SHA256 access tokens.
 *
 * <p>Token claims:
 * <ul>
 *   <li>{@code sub} — user ID (Long)</li>
 *   <li>{@code email} — user email</li>
 *   <li>{@code role} — role name (admin/editor/author)</li>
 *   <li>{@code type} — "access" or "refresh"</li>
 *   <li>{@code jti} — JWT ID (UUID, for revocation tracking)</li>
 * </ul>
 *
 * <p>Refresh tokens are random opaque strings (UUID), NOT JWTs —
 * they're stored in {@code sessions} table so they can be revoked.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTtl;
    private final long refreshTtl;

    public JwtService(BlogProperties props) {
        // Secret must be ≥ 256 bits (32 bytes) for HS256.
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "blog.jwt.secret must be set and ≥ 32 chars (256 bits). " +
                    "Configure JWT_SECRET env var.");
        }
        // Try base64 first; fall back to raw bytes.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtl = props.getJwt().getAccessTokenTtlSeconds();
        this.refreshTtl = props.getJwt().getRefreshTokenTtlSeconds();
    }

    /**
     * Issue a short-lived access token (JWT).
     */
    public String issueAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtl);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claims(Map.of(
                        "email", email,
                        "role", role,
                        "type", "access"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a random opaque refresh token (NOT a JWT).
     * Stored in sessions table — can be revoked individually.
     */
    public String issueRefreshToken() {
        return UUID.randomUUID().toString() + "." + UUID.randomUUID().toString();
    }

    public long getRefreshTtlSeconds() {
        return refreshTtl;
    }

    /**
     * Parse + verify JWT. Returns claims if valid; throws if expired/invalid.
     */
    public Claims parseAndVerify(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("JWT verification failed: {}", e.getMessage());
            throw e;
        }
    }
}
