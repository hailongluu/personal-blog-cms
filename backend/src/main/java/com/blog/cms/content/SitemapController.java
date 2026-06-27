package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Public XML sitemap for SEO.
 * Spec: https://www.sitemaps.org/protocol.html
 *
 * Routes:
 *   /                       (homepage, daily)
 *   /blog                   (post listing, daily)
 *   /about, /now, /newsletter (static, monthly)
 *   /blog/:slug             (published posts, weekly)
 *   /topic/:slug            (active topics, weekly)
 *   /project/:slug          (active projects, monthly)
 *
 * Also exposes /robots.txt (text/plain) pointing to this sitemap.
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class SitemapController {

    private static final DateTimeFormatter W3C = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private static final int POST_FETCH_LIMIT = 1000;

    private final PublicPostService publicPostService;
    private final TopicRepository topicRepository;
    private final ProjectRepository projectRepository;
    private final BlogProperties blogProperties;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String base = stripTrailingSlash(blogProperties.getSite().getUrl());

        List<UrlEntry> entries = new ArrayList<>();

        // Static routes
        entries.add(url(base + "/",            "1.0",  "daily"));
        entries.add(url(base + "/blog",        "0.9",  "daily"));
        entries.add(url(base + "/about",       "0.6",  "monthly"));
        entries.add(url(base + "/now",         "0.5",  "monthly"));
        entries.add(url(base + "/newsletter",  "0.7",  "monthly"));

        // Dynamic routes — published posts
        try {
            ApiResponse<List<PostResponse>> resp = publicPostService
                    .listPublishedPosts(null, null, 1, POST_FETCH_LIMIT);
            List<PostResponse> posts = resp != null ? resp.getData() : List.of();
            for (PostResponse p : posts) {
                if (p.getSlug() == null) continue;
                String loc = base + "/blog/" + p.getSlug();
                Instant lastmod = pickLastmod(p);
                entries.add(url(loc, "0.8", "weekly", lastmod));
            }
        } catch (Exception e) {
            log.warn("Sitemap: failed to fetch posts — {}", e.getMessage());
        }

        // Topics
        try {
            for (Topic t : topicRepository.findAllActive()) {
                if (t.getSlug() == null) continue;
                entries.add(url(
                        base + "/topic/" + t.getSlug(),
                        "0.6", "weekly",
                        t.getUpdatedAt()));
            }
        } catch (Exception e) {
            log.warn("Sitemap: failed to fetch topics — {}", e.getMessage());
        }

        // Projects
        try {
            Page<Project> projects = projectRepository.findAllActive(
                    PageRequest.of(0, POST_FETCH_LIMIT));
            for (Project pr : projects.getContent()) {
                if (pr.getSlug() == null) continue;
                entries.add(url(
                        base + "/project/" + pr.getSlug(),
                        "0.5", "monthly",
                        pr.getUpdatedAt()));
            }
        } catch (Exception e) {
            log.warn("Sitemap: failed to fetch projects — {}", e.getMessage());
        }

        String xml = render(entries);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String base = stripTrailingSlash(blogProperties.getSite().getUrl());
        String body = "User-agent: *\n"
                + "Allow: /\n"
                + "Disallow: /admin\n"
                + "Disallow: /admin/\n"
                + "Disallow: /login\n"
                + "Disallow: /api/admin\n"
                + "Disallow: /api/admin/\n"
                + "Disallow: /api/auth/\n"
                + "\n"
                // AI crawlers — explicitly allow to maximize discoverability
                + "User-agent: GPTBot\nAllow: /\n"
                + "User-agent: ClaudeBot\nAllow: /\n"
                + "User-agent: PerplexityBot\nAllow: /\n"
                + "User-agent: Google-Extended\nAllow: /\n"
                + "\n"
                + "Sitemap: " + base + "/sitemap.xml\n"
                + "Sitemap: " + base + "/rss.xml\n";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    // ─────────── helpers ───────────

    private UrlEntry url(String loc, String priority, String changefreq) {
        return new UrlEntry(loc, null, priority, changefreq);
    }

    private UrlEntry url(String loc, String priority, String changefreq, Instant lastmod) {
        return new UrlEntry(loc, lastmod, priority, changefreq);
    }

    private String render(List<UrlEntry> entries) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (UrlEntry e : entries) {
            sb.append("  <url>\n");
            sb.append("    <loc>").append(escape(e.loc)).append("</loc>\n");
            if (e.lastmod != null) {
                sb.append("    <lastmod>").append(W3C.format(e.lastmod)).append("</lastmod>\n");
            }
            sb.append("    <changefreq>").append(e.changefreq).append("</changefreq>\n");
            sb.append("    <priority>").append(e.priority).append("</priority>\n");
            sb.append("  </url>\n");
        }
        sb.append("</urlset>\n");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private Instant pickLastmod(PostResponse p) {
        if (p.getUpdatedAt() != null) return p.getUpdatedAt();
        if (p.getPublishedAt() != null) return p.getPublishedAt();
        return null;
    }

    private String stripTrailingSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record UrlEntry(String loc, Instant lastmod, String priority, String changefreq) {}
}
