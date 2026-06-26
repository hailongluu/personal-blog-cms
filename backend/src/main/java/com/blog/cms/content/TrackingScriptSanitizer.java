package com.blog.cms.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict allowlist sanitizer for custom head/body tracking scripts.
 *
 * Policy (decided with stakeholders — Option A "Strict"):
 *   - Allowed tags: script (only with allowlisted src), style, noscript, meta, link
 *   - Disallowed tags: iframe, object, embed, form, input, button, base, frame, frameset
 *   - All event handlers (onclick, onerror, onload, etc.) stripped
 *   - Disallowed URL schemes: javascript:, data:, vbscript:
 *   - For script src: only allowlist domains (Google, Facebook, TikTok, Microsoft)
 *   - Inline script content (between <script>...</script>) is PRESERVED — needed for tracking init code
 *
 * Validation methods for tracking IDs use strict regex patterns.
 */
@Component
public class TrackingScriptSanitizer {

    // ── ID validation regexes ─────────────────────────────────────
    private static final Pattern GA4_ID = Pattern.compile("^G-[A-Z0-9]{4,12}$");
    private static final Pattern GTM_ID = Pattern.compile("^GTM-[A-Z0-9]{4,12}$");
    private static final Pattern FB_PIXEL_ID = Pattern.compile("^\\d{15,20}$");
    private static final Pattern TIKTOK_PIXEL_ID = Pattern.compile("^C[A-Z0-9]{14,20}$");

    // ── Allowlist domains for <script src="..."> ──────────────────
    private static final Set<String> SCRIPT_SRC_ALLOWLIST = Set.of(
        // Google
        "www.googletagmanager.com",
        "www.google-analytics.com",
        "analytics.google.com",
        // Facebook / Meta
        "connect.facebook.net",
        // TikTok
        "analytics.tiktok.com",
        // Microsoft Clarity
        "www.clarity.ms",
        // Hotjar
        "static.hotjar.com",
        "script.hotjar.com",
        // Plausible / Umami / PostHog self-host-friendly
        "plausible.io",
        "umami.is"
    );

    // ── Jsoup safelist configured for our policy ──────────────────
    private static final Safelist HEAD_SAFELIST = buildHeadSafelist();
    private static final Safelist CSS_SAFELIST = buildCssSafelist();

    /**
     * Sanitize HTML to be injected in &lt;head&gt; or &lt;body&gt;.
     * Allowed tags: script (allowlist src only), style, noscript, meta, link.
     * Inline script bodies (init code) are preserved.
     */
    public String sanitizeHead(String html) {
        if (html == null || html.isBlank()) return "";
        // Parse as fragment (no html/body wrapper); preserve case for tags
        Document.OutputSettings outputSettings = new Document.OutputSettings()
            .prettyPrint(false)
            .outline(false);

        // Jsoup sanitize with safelist
        String cleaned = Jsoup.clean(
            html,
            "",
            HEAD_SAFELIST,
            outputSettings
        );

        // Post-process: enforce script src allowlist (Jsoup safelist alone is too permissive)
        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings(outputSettings);

        for (Element script : doc.select("script[src]")) {
            String src = script.attr("src");
            if (!isAllowlistedScriptSrc(src)) {
                script.remove();
            }
        }

        // Strip any remaining event handlers (defense in depth)
        doc.select("*").forEach(el -> {
            el.attributes().forEach(attr -> {
                String key = attr.getKey().toLowerCase();
                if (key.startsWith("on")) {
                    el.removeAttr(attr.getKey());
                }
            });
            // Strip dangerous URL schemes from href/src
            for (String attr : new String[]{"href", "src"}) {
                String val = el.attr(attr).toLowerCase().trim();
                if (val.startsWith("javascript:") || val.startsWith("vbscript:") || val.startsWith("data:")) {
                    el.removeAttr(attr);
                }
            }
        });

        return doc.body().html();
    }

    /**
     * Sanitize CSS — only &lt;style&gt; tags with text content (no HTML inside).
     */
    public String sanitizeCss(String css) {
        if (css == null || css.isBlank()) return "";
        Document.OutputSettings outputSettings = new Document.OutputSettings()
            .prettyPrint(false);
        // No HTML tags in CSS; just strip any dangerous sequences
        String cleaned = Jsoup.clean(css, "", CSS_SAFELIST, outputSettings);
        // Defense in depth: ensure no closing </style> breaks out
        cleaned = cleaned.replace("</style>", "");
        return cleaned.trim();
    }

    // ── ID validators ─────────────────────────────────────────────

    public boolean isValidGa4Id(String id) {
        return id != null && GA4_ID.matcher(id.trim()).matches();
    }

    public boolean isValidGtmId(String id) {
        return id != null && GTM_ID.matcher(id.trim()).matches();
    }

    public boolean isValidFbPixelId(String id) {
        return id != null && FB_PIXEL_ID.matcher(id.trim()).matches();
    }

    public boolean isValidTiktokPixelId(String id) {
        return id != null && TIKTOK_PIXEL_ID.matcher(id.trim()).matches();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private boolean isAllowlistedScriptSrc(String src) {
        if (src == null || src.isBlank()) return false;
        String lower = src.toLowerCase().trim();
        // Allow relative URLs (inline scripts have no src)
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("//")) {
            return true; // relative path is safe
        }
        try {
            String host = lower.contains("://")
                ? java.net.URI.create(lower.startsWith("//") ? "https:" + lower : lower).getHost()
                : null;
            if (host == null) return false;
            host = host.toLowerCase();
            for (String allowed : SCRIPT_SRC_ALLOWLIST) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static Safelist buildHeadSafelist() {
        return Safelist.none()
            .addTags("script", "style", "meta", "link")
            // Allow common attrs on these tags
            .addAttributes("script", "src", "async", "defer", "type", "id", "nonce", "crossorigin")
            .addAttributes("link", "rel", "href", "type", "as", "crossorigin")
            .addAttributes("meta", "name", "content", "property", "charset", "http-equiv")
            .addAttributes("style", "type", "media")
            // Allow URL protocols: https, http, mailto (NO javascript:, data:, vbscript:)
            .addProtocols("script", "src", "http", "https")
            .addProtocols("link", "href", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto");
    }

    private static Safelist buildCssSafelist() {
        return Safelist.none()
            .addTags("style")
            .addAttributes("style", "type", "media");
    }
}
