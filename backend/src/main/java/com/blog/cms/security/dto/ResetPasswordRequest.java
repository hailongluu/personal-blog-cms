package com.blog.cms.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/admin/auth/reset-password.
 *
 * <p>Token comes from the email link. New password must be 8-100 chars per SPEC §7.6.
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100, message = "Password must be 8-100 characters") String newPassword
) {}
