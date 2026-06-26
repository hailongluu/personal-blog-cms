package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD tests for SettingsService — Tracking Scripts + Custom Scripts/CSS section.
 * Extends existing CRUD coverage with tracking-script-specific validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsService — Tracking scripts config")
class SettingsServiceTest {

    @Mock
    private SettingsRepository settingsRepository;

    @Mock
    private TrackingScriptSanitizer sanitizer;

    @InjectMocks
    private SettingsService settingsService;

    @Nested
    @DisplayName("Update tracking.* keys")
    class TrackingKeys {

        @Test
        @DisplayName("Valid GA4 ID is accepted and persisted")
        void updateGa4_validId_persisted() {
            when(sanitizer.isValidGa4Id("G-ABC123DEF4")).thenReturn(true);
            when(settingsRepository.findById("tracking.ga4_measurement_id"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.ga4_measurement_id", "G-ABC123DEF4");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
            verify(settingsRepository, times(1)).save(any(Setting.class));
        }

        @Test
        @DisplayName("Invalid GA4 ID (no G- prefix) is rejected with error")
        void updateGa4_invalidId_rejected() {
            when(sanitizer.isValidGa4Id("INVALID123")).thenReturn(false);

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.ga4_measurement_id", "INVALID123");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNotNull(response.getError());
            assertTrue(response.getError().contains("tracking.ga4_measurement_id"));
            verify(settingsRepository, never()).save(any(Setting.class));
        }

        @Test
        @DisplayName("Valid GTM Container ID is accepted")
        void updateGtm_validId_persisted() {
            when(sanitizer.isValidGtmId("GTM-ABC123")).thenReturn(true);
            when(settingsRepository.findById("tracking.gtm_container_id"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.gtm_container_id", "GTM-ABC123");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
        }

        @Test
        @DisplayName("Invalid GTM Container ID is rejected")
        void updateGtm_invalidId_rejected() {
            when(sanitizer.isValidGtmId("ABC123")).thenReturn(false);

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.gtm_container_id", "ABC123");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Valid Facebook Pixel ID is accepted")
        void updateFbPixel_validId_persisted() {
            when(sanitizer.isValidFbPixelId("123456789012345")).thenReturn(true);
            when(settingsRepository.findById("tracking.fb_pixel_id"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.fb_pixel_id", "123456789012345");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
        }

        @Test
        @DisplayName("Invalid Facebook Pixel ID (non-digits) is rejected")
        void updateFbPixel_invalidId_rejected() {
            when(sanitizer.isValidFbPixelId("12345abc6789012")).thenReturn(false);

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.fb_pixel_id", "12345abc6789012");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Valid TikTok Pixel ID is accepted")
        void updateTiktok_validId_persisted() {
            when(sanitizer.isValidTiktokPixelId("C12345ABCDE6789")).thenReturn(true);
            when(settingsRepository.findById("tracking.tiktok_pixel_id"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.tiktok_pixel_id", "C12345ABCDE6789");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
        }

        @Test
        @DisplayName("Custom head script with <iframe> is sanitized before persist")
        void updateCustomHead_withIframe_isSanitized() {
            when(sanitizer.sanitizeHead(anyString())).thenReturn("<style>body{color:red}</style>");
            when(settingsRepository.findById("custom.head_scripts"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> {
                Setting s = inv.getArgument(0);
                // Verify sanitizer stripped iframe
                assertFalse(s.getValue().contains("<iframe"));
                return s;
            });

            Map<String, String> updates = new HashMap<>();
            updates.put("custom.head_scripts",
                "<style>body{color:red}</style><iframe src='https://evil.com'></iframe>");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
            verify(settingsRepository, times(1)).save(any(Setting.class));
        }

        @Test
        @DisplayName("Custom CSS is stored without sanitization")
        void updateCustomCss_storedAsIs() {
            when(sanitizer.sanitizeCss(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(settingsRepository.findById("custom.css"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> {
                Setting s = inv.getArgument(0);
                assertEquals("body{color:red}h1{font-size:32px}", s.getValue());
                return s;
            });

            Map<String, String> updates = new HashMap<>();
            updates.put("custom.css", "body{color:red}h1{font-size:32px}");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
        }

        @Test
        @DisplayName("Consent mode accepts only: none, basic, full")
        void updateConsentMode_invalidValue_rejected() {
            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.consent_mode", "invalid_mode");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Consent mode 'basic' is accepted")
        void updateConsentMode_basic_accepted() {
            when(settingsRepository.findById("tracking.consent_mode"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> updates = new HashMap<>();
            updates.put("tracking.consent_mode", "basic");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
        }
    }

    @Nested
    @DisplayName("Public settings — exclude admin-only keys")
    class PublicSettingsFilter {

        @Test
        @DisplayName("Public settings excludes tracking.gtag_enabled (admin only)")
        void publicSettings_excludesAdminOnlyKeys() {
            Setting adminOnly = Setting.builder()
                .key("tracking.gtag_enabled")
                .value("true")
                .valueType("boolean")
                .isPublic(false)
                .build();
            Setting publicSetting = Setting.builder()
                .key("tracking.ga4_measurement_id")
                .value("G-ABC123")
                .valueType("string")
                .isPublic(true)
                .build();

            when(settingsRepository.findByIsPublicTrue())
                .thenReturn(List.of(publicSetting));

            ApiResponse<Map<String, Object>> response = settingsService.getPublicSettings();

            Map<String, Object> data = response.getData();
            assertNotNull(data);
            assertTrue(data.containsKey("tracking.ga4_measurement_id"));
            assertFalse(data.containsKey("tracking.gtag_enabled"));
        }

        @Test
        @DisplayName("Admin settings includes both public + admin-only keys")
        void allSettings_includesAllKeys() {
            Setting s1 = Setting.builder().key("tracking.ga4_measurement_id").value("G-ABC123").valueType("string").isPublic(true).build();
            Setting s2 = Setting.builder().key("tracking.gtag_enabled").value("true").valueType("boolean").isPublic(false).build();

            when(settingsRepository.findAll()).thenReturn(List.of(s1, s2));

            ApiResponse<Map<String, Object>> response = settingsService.getAllSettings();

            Map<String, Object> data = response.getData();
            assertTrue(data.containsKey("tracking.ga4_measurement_id"));
            assertTrue(data.containsKey("tracking.gtag_enabled"));
        }
    }

    @Nested
    @DisplayName("Backward compatibility — existing keys still work")
    class BackwardCompatibility {

        @Test
        @DisplayName("site.title still updates correctly")
        void siteTitle_stillWorks() {
            when(settingsRepository.findById("site.title"))
                .thenReturn(java.util.Optional.empty());
            when(settingsRepository.save(any(Setting.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> updates = new HashMap<>();
            updates.put("site.title", "My Blog");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNull(response.getError());
        }

        @Test
        @DisplayName("Unknown key returns validation error")
        void unknownKey_returnsError() {
            Map<String, String> updates = new HashMap<>();
            updates.put("invalid.key", "value");

            ApiResponse<Map<String, Object>> response = settingsService.updateSettings(updates, 1L);

            assertNotNull(response.getError());
            assertTrue(response.getError().contains("invalid.key"));
        }
    }
}
