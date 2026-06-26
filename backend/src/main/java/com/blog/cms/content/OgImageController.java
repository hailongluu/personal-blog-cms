package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * OG Image generator — renders an SVG cover image dynamically.
 *
 * Why SVG: lightweight (no Chrome headless / Playwright / heavy dependencies),
 * always up-to-date with current post data, perfect for crawlers & social
 * sharing (Facebook, Twitter, LinkedIn all accept SVG via &lt;img&gt;).
 *
 * To convert to PNG, run once:
 *   curl 'https://news.luuhailong.com/api/public/og/post-slug' > og.svg
 *   rsvg-convert -w 1200 -h 630 og.svg > og.png
 *
 * Or use the frontend at build-time to bake static OG.png assets.
 *
 * Spec: https://ogp.me/ (1200x630 recommended)
 */
@RestController
@RequestMapping("/api/public/og")
@RequiredArgsConstructor
public class OgImageController {

    private final PublicPostService publicPostService;
    private final BlogProperties blogProperties;

    /**
     * Generate SVG OG image for a post by slug.
     * Example: GET /api/public/og/ai-agent-enterprise-workflow
     */
    @GetMapping(value = "/{slug}.svg", produces = "image/svg+xml")
    public ResponseEntity<String> generateOgImage(@PathVariable String slug) {
        ApiResponse<PostResponse> postResponse = publicPostService.getPublishedPostBySlug(slug);
        PostResponse post = postResponse != null ? postResponse.getData() : null;
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        String svg = OgImageRenderer.render(post, blogProperties);
        return ResponseEntity.ok()
            .header("Cache-Control", "public, max-age=86400") // 24h
            .header("Content-Type", "image/svg+xml; charset=utf-8")
            .body(svg);
    }

    /**
     * Generic OG image (used for home/about) with custom title.
     * Example: GET /api/public/og/site?title=About&subtitle=Personal blog of Lưu Hải Long
     */
    @GetMapping(value = "/site.svg", produces = "image/svg+xml")
    public ResponseEntity<String> generateSiteOgImage(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String subtitle) {
        String svg = OgImageRenderer.renderGeneric(
            title.isBlank() ? Optional.ofNullable(blogProperties.getSite().getTitle()).orElse("Personal Blog") : title,
            subtitle.isBlank() ? Optional.ofNullable(blogProperties.getSite().getDescription()).orElse("") : subtitle,
            blogProperties
        );
        return ResponseEntity.ok()
            .header("Cache-Control", "public, max-age=3600")
            .header("Content-Type", "image/svg+xml; charset=utf-8")
            .body(svg);
    }
}
