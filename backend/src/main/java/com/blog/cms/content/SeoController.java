package com.blog.cms.content;

import com.blog.cms.config.BlogProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class SeoController {

    private final PostRepository postRepository;
    private final TopicRepository topicRepository;
    private final ProjectRepository projectRepository;
    private final BlogProperties blogProperties;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robotsTxt(HttpServletRequest request) {
        String baseUrl = resolveBaseUrl(request);
        return """
            User-agent: Googlebot
            Allow: /
            Disallow: /api/admin/

            User-agent: *
            Disallow: /api/admin/
            Disallow: /api/admin/*

            Sitemap: %s/sitemap.xml
            """.formatted(baseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapXml(HttpServletRequest request) {
        String baseUrl = resolveBaseUrl(request);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Homepage
        sb.append("  <url>\n");
        sb.append("    <loc>").append(baseUrl).append("</loc>\n");
        sb.append("    <changefreq>daily</changefreq>\n");
        sb.append("    <priority>1.0</priority>\n");
        sb.append("  </url>\n");

        // Published posts
        var posts = postRepository.findPublishedPosts(
            java.time.Instant.now(), null, null,
            org.springframework.data.domain.PageRequest.of(0, 500));
        for (var post : posts.getContent()) {
            sb.append("  <url>\n");
            sb.append("    <loc>").append(baseUrl).append("/blog/").append(post.getSlug()).append("</loc>\n");
            if (post.getUpdatedAt() != null) {
                sb.append("    <lastmod>").append(dateFmt.format(post.getUpdatedAt())).append("</lastmod>\n");
            }
            sb.append("    <changefreq>weekly</changefreq>\n");
            sb.append("    <priority>0.8</priority>\n");
            sb.append("  </url>\n");
        }

        // Topics
        var topics = topicRepository.findAllActive();
        for (var topic : topics) {
            sb.append("  <url>\n");
            sb.append("    <loc>").append(baseUrl).append("/topics/").append(topic.getSlug()).append("</loc>\n");
            sb.append("    <changefreq>weekly</changefreq>\n");
            sb.append("    <priority>0.6</priority>\n");
            sb.append("  </url>\n");
        }

        // Projects
        var projects = projectRepository.findAllActive(
            org.springframework.data.domain.PageRequest.of(0, 500));
        for (var project : projects.getContent()) {
            sb.append("  <url>\n");
            sb.append("    <loc>").append(baseUrl).append("/projects/").append(project.getSlug()).append("</loc>\n");
            sb.append("    <changefreq>monthly</changefreq>\n");
            sb.append("    <priority>0.7</priority>\n");
            sb.append("  </url>\n");
        }

        sb.append("</urlset>\n");
        return sb.toString();
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String configured = blogProperties.getSite().getUrl();
        // Use configured URL if set and not a localhost default.
        // Falls back to request-derived URL when running behind a reverse proxy
        // (Cloudflare, nginx) that sets Host to the public domain.
        if (configured != null && !configured.isBlank()
                && !configured.startsWith("http://localhost")
                && !configured.startsWith("http://127.0.0.1")) {
            return configured.replaceAll("/$", "");
        }
        // Fallback: derive from request (handles Cloudflare + nginx proxy)
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) host = request.getServerName();
        // Behind Cloudflare, port is the standard https port — never include.
        return scheme + "://" + host;
    }
}
