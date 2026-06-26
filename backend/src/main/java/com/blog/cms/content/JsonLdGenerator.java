package com.blog.cms.content;

import com.blog.cms.config.BlogProperties;
import com.blog.cms.user.User;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates Schema.org BlogPosting JSON-LD for a Post.
 * Spec: https://schema.org/BlogPosting
 *       https://developers.google.com/search/docs/appearance/structured-data/article
 *
 * Used by PostResponse.from() so every post detail endpoint returns a
 * ready-to-inject &lt;script type="application/ld+json"&gt; block.
 */
public final class JsonLdGenerator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private JsonLdGenerator() {}

    public static String forBlogPost(Post post) {
        if (post == null) return null;
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "BlogPosting");

        // Headline + description
        String headline = post.getMetaTitle() != null && !post.getMetaTitle().isBlank()
            ? post.getMetaTitle()
            : post.getTitle();
        root.put("headline", truncate(headline, 110));
        if (post.getMetaDescription() != null && !post.getMetaDescription().isBlank()) {
            root.put("description", truncate(post.getMetaDescription(), 250));
        } else if (post.getExcerpt() != null) {
            root.put("description", truncate(post.getExcerpt(), 250));
        }

        // Image (OG image preferred, fallback cover)
        String imageUrl = post.getOgImageUrl() != null && !post.getOgImageUrl().isBlank()
            ? post.getOgImageUrl()
            : post.getCoverImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            root.put("image", List.of(imageUrl));
        }

        // Author
        if (post.getAuthor() != null) {
            User authorUser = post.getAuthor();
            Map<String, Object> author = new LinkedHashMap<>();
            author.put("@type", "Person");
            author.put("name", authorUser.getDisplayName());
            if (authorUser.getAvatarUrl() != null) {
                author.put("image", authorUser.getAvatarUrl());
            }
            root.put("author", author);
        }

        // Publisher
        Map<String, Object> publisher = new LinkedHashMap<>();
        publisher.put("@type", "Organization");
        publisher.put("name", "Lưu Hải Long News"); // TODO: pull from settings
        root.put("publisher", publisher);

        // Dates (ISO 8601)
        if (post.getPublishedAt() != null) {
            root.put("datePublished", ISO.format(post.getPublishedAt()));
        }
        if (post.getUpdatedAt() != null) {
            root.put("dateModified", ISO.format(post.getUpdatedAt()));
        }

        // Main entity of page (canonical)
        if (post.getCanonicalUrl() != null && !post.getCanonicalUrl().isBlank()) {
            Map<String, Object> mainEntity = new LinkedHashMap<>();
            mainEntity.put("@type", "WebPage");
            mainEntity.put("@id", post.getCanonicalUrl());
            root.put("mainEntityOfPage", mainEntity);
        }

        // Article section (topic)
        if (post.getTopic() != null) {
            root.put("articleSection", post.getTopic().getName());
        }

        // Keywords (tags)
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            List<String> keywords = new ArrayList<>();
            post.getTags().forEach(t -> keywords.add(t.getName()));
            root.put("keywords", String.join(", ", keywords));
        }

        // Word count (approximate from markdown)
        if (post.getContentMarkdown() != null) {
            int words = post.getContentMarkdown().split("\\s+").length;
            root.put("wordCount", words);
        }

        // Serialize (manual JSON to avoid Jackson dependency in this util)
        return toJson(root);
    }

    // ─── helpers ─────────────────────────────────────────────

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            return sb.append("]").toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escape(e.getKey().toString())).append("\":");
                sb.append(toJson(e.getValue()));
            }
            return sb.append("}").toString();
        }
        return "\"" + escape(value.toString()) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
