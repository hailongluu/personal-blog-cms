package com.blog.cms.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * PasswordResetToken — single-use token for password reset flow (SPEC §7).
 *
 * <p>Flow:
 * <ol>
 *   <li>User requests reset via /api/admin/auth/forgot-password</li>
 *   <li>AuthService generates random token, persists with 1-hour TTL</li>
 *   <li>Token is sent via email (or logged in dev mode)</li>
 *   <li>User submits new password via /api/admin/auth/reset-password</li>
 *   <li>Service validates token, updates password, marks token used</li>
 *   <li>All existing sessions are revoked (defense in depth)</li>
 * </ol>
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_password_reset_user"))
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;  // SHA-256 hash of the token (token never stored in plain)

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }
}
