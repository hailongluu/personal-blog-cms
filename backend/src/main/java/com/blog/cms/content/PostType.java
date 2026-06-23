package com.blog.cms.content;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Post type — SPEC §8.3.4
 *
 * Drives UX (different layouts / icons) and filtering in admin + public APIs.
 * Stored as VARCHAR with a CHECK constraint (see V3 migration) to keep
 * migrations simple.
 */
public enum PostType {
    ESSAY,
    RESEARCH_BRIEF,
    FIELD_NOTE,
    BUILD_LOG,
    PLAYBOOK,
    REVIEW,
    PERSONAL_LOG;

    /** Lowercase variant for DB and external API payloads. */
    public String value() {
        return name().toLowerCase();
    }

    /** Parse case-insensitive; returns null when input is blank or unknown. */
    public static PostType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PostType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** All valid lowercase values — used by validation / DTO. */
    public static Set<String> allValues() {
        return Stream.of(values()).map(PostType::value).collect(Collectors.toUnmodifiableSet());
    }
}
