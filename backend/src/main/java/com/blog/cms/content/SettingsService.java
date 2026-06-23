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

    /** Allowed keys + their types for validation. */
    private static final Map<String, String> ALLOWED_KEYS = Map.ofEntries(
        Map.entry("site.title", "string"),
        Map.entry("site.description", "string"),
        Map.entry("site.author", "string"),
        Map.entry("site.author_bio", "string"),
        Map.entry("social.github", "string"),
        Map.entry("social.linkedin", "string"),
        Map.entry("social.x", "string"),
        Map.entry("social.youtube", "string"),
        Map.entry("site.email", "string")
    );

    /**
     * Return public settings as a flat Map&lt;String, Object&gt;.
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

            String type = ALLOWED_KEYS.get(key);
            Setting setting = settingsRepository.findById(key)
                .orElseGet(() -> Setting.builder()
                    .key(key)
                    .valueType(type)
                    .isPublic(true)
                    .build());

            setting.setValue(newValue);
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

    // ─── helpers ──────────────────────────────────────────────

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
            case "json" -> value; // keep as raw string
            default -> value; // string
        };
    }
}
