package com.blog.cms.security.dto;

import java.time.Instant;

public record AuthResponse(
        Long userId,
        String email,
        String displayName,
        String role,
        Long accessExpiresAt,
        Long refreshExpiresAt
) {}
