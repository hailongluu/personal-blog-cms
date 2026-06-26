package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private final SettingsRepository settingsRepository;
    private final TrackingScriptSanitizer sanitizer;

    /**
     * Allowed keys + their types for validation.
     * Sections: site, author, social, contact, tracking, custom.
     */
    private static final Map<String, String> ALLOWED_KEYS = Map.ofEntries(
        // ── Site (existing) ────────────────────────────────────
        Map.entry("site.title", "string"),
        Map.entry("site.description", "string"),
        Map.entry("site.author", "string"),
        Map.entry("site.author_bio", "string"),
        // ── Social (existing) ──────────────────────────────────
        Map.entry("social.github", "string"),
        Map.entry("social.linkedin", "string"),
        Map.entry("social.x", "string"),
        Map.entry("social.youtube", "string"),
        // ── Contact (existing) ─────────────────────────────────
        Map.entry("site.email", "string"),
        // ── Tracking scripts (new) ────────────────────────────
        Map.entry("tracking.ga4_measurement_id", "string"),
        Map.entry("tracking.gtm_container_id", "string"),
        Map.entry("tracking.fb_pixel_id", "string"),
        Map.entry("tracking.tiktok_pixel_id", "string"),
        Map.entry("tracking.gtag_enabled", "boolean"),
        Map.entry("tracking.fb_enabled", "boolean"),
        Map.entry("tracking.tiktok_enabled", "boolean"),
        Map.entry("tracking.consent_mode", "string"),
        // ── Custom scripts/CSS (new) ───────────────────────────
        Map.entry("custom.head_scripts", "html"),
        Map.entry("custom.body_start_scripts", "html"),
        Map.entry("custom.body_end_scripts", "html"),
        Map.entry("custom.css", "css")
    );

    /**
     * Return public settings as a flat Map&lt;String, Object&gt;.
     * (tracking.gtag_enabled / .fb_enabled / .tiktok_enabled are admin-only)
     */
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getPublicSettings() {
        List<Setting> publicSettings = settingsRepository.findByIsPublicTrue();
        Map<String, Object> result = publicSettings.stream()
            .collect(Collectors.toMap(
                Setting::getKey,
                s -> coerceValue(s.getValue(), s.getValueType()),
                (a, b) -> b,
                LinkedHashMap::new
            ));
        return ApiResponse.ok(result);
    }

    /**
     * Return all settings (admin).
     */
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getAllSettings() {
        List<Setting> all = settingsRepository.findAll();
        Map<String, Object> result = all.stream()
            .collect(Collectors.toMap(
                Setting::getKey,
                s -> coerceValue(s.getValue(), s.getValueType()),
                (a, b) -> b,
                LinkedHashMap::new
            ));
        return ApiResponse.ok(result);
    }

    /**
     * Bulk-update settings from a Map&lt;String, String&gt;.
     * Only updates keys present in ALLOWED_KEYS.
     * Validates IDs by type, sanitizes HTML/CSS by type.
     */
    public ApiResponse<Map<String, Object>> updateSettings(Map<String, String> updates, Long userId) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (var entry : updates.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();

            if (!ALLOWED_KEYS.containsKey(key)) {
                errors.put(key, "Unknown setting key: " + key);
                continue;
            }
            if (newValue == null) {
                errors.put(key, "Value must not be null");
                continue;
            }

            // ── Per-key validation ─────────────────────────────
            String validationError = validateValue(key, newValue);
            if (validationError != null) {
                errors.put(key, validationError);
                continue;
            }

            String type = ALLOWED_KEYS.get(key);
            String valueToStore = sanitizeForStorage(key, type, newValue);

            Setting setting = settingsRepository.findById(key)
                .orElseGet(() -> Setting.builder()
                    .key(key)
                    .valueType(type)
                    .isPublic(isPublicKey(key))
                    .build());

            setting.setValue(valueToStore);
            setting.setUpdatedBy(userId);
            setting.setUpdatedAt(Instant.now());
            settingsRepository.save(setting);
        }

        if (!errors.isEmpty()) {
            return ApiResponse.error("Validation errors: " + errors);
        }

        log.info("Settings updated by user {}: {}", userId, updates.keySet());
        return getAllSettings();
    }

    // ─── Validation ──────────────────────────────────────────────

    /**
     * Returns null if valid, error message if invalid.
     */
    private String validateValue(String key, String value) {
        if (value.isEmpty()) {
            // Empty allowed (clears the setting)
            return null;
        }
        return switch (key) {
            case "tracking.ga4_measurement_id" ->
                sanitizer.isValidGa4Id(value) ? null
                    : "Invalid GA4 ID. Expected format: G-XXXXXXXXXX (e.g., G-ABC123DEF4)";
            case "tracking.gtm_container_id" ->
                sanitizer.isValidGtmId(value) ? null
                    : "Invalid GTM Container ID. Expected format: GTM-XXXXXXX";
            case "tracking.fb_pixel_id" ->
                sanitizer.isValidFbPixelId(value) ? null
                    : "Invalid Facebook Pixel ID. Expected: 15-20 digits (numbers only)";
            case "tracking.tiktok_pixel_id" ->
                sanitizer.isValidTiktokPixelId(value) ? null
                    : "Invalid TikTok Pixel ID. Expected format: CXXXXXXXXXXXXXXX";
            case "tracking.consent_mode" -> {
                String v = value.toLowerCase();
                yield (v.equals("none") || v.equals("basic") || v.equals("full"))
                    ? null
                    : "Invalid consent_mode. Allowed: none, basic, full";
            }
            default -> null; // other keys pass through
        };
    }

    /**
     * Sanitize value before persisting to DB.
     * - html type: run through strict sanitizer (allowlist tags)
     * - css type: strip dangerous sequences
     * - others: pass through
     */
    private String sanitizeForStorage(String key, String type, String value) {
        if (value.isEmpty()) return value;
        return switch (type) {
            case "html" -> sanitizer.sanitizeHead(value);
            case "css" -> sanitizer.sanitizeCss(value);
            default -> value;
        };
    }

    /**
     * Whether a key is exposed via /api/public/settings.
     * Admin-only keys (toggles) are NOT public.
     */
    private boolean isPublicKey(String key) {
        return switch (key) {
            case "tracking.gtag_enabled", "tracking.fb_enabled", "tracking.tiktok_enabled" -> false;
            default -> true;
        };
    }

    // ─── helpers ─────────────────────────────────────────────

    /** Coerce string value to its declared type for the response map. */
    private Object coerceValue(String value, String valueType) {
        if (value == null) return null;
        return switch (valueType) {
            case "number" -> {
                try {
                    yield value.contains(".") ? Double.parseDouble(value) : Long.parseLong(value);
                } catch (NumberFormatException e) {
                    yield value;
                }
            }
            case "boolean" -> "true".equalsIgnoreCase(value);
            case "json" -> value;
            default -> value; // string / html / css — keep as raw string
        };
    }
}
