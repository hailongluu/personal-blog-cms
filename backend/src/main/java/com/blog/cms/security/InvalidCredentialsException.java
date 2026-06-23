package com.blog.cms.security;

/**
 * Thrown when authentication fails (invalid credentials, expired token, etc).
 * Caught by GlobalExceptionHandler → 401 with INVALID_CREDENTIALS code.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
