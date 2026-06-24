package com.blog.cms.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/admin/auth/forgot-password.
 *
 * <p>Always returns 200 even for unknown emails to prevent user enumeration.
 */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {}
