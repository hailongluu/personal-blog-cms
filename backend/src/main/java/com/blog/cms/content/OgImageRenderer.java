package com.blog.cms.content;

import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.PostResponse;

import java.util.Optional;

/**
 * SVG-based OG image renderer. Produces a 1200x630 SVG suitable for
 * social media (Facebook, Twitter, LinkedIn).
 *
 * Design: warm light theme per sếp Long's design preference
 *   - bg: cream (#f5f0ea)
 *   - text: dark (#2d2d3a)
 *   - accent: teal (#0d9488) + coral (#fb7185)
 *   - author byline at bottom
 *   - site logo top-left
 *   - title centered with subtle coral underline
 */
public final class OgImageRenderer {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 630;

    private OgImageRenderer() {}

    public static String render(PostResponse post, BlogProperties props) {
        String title = truncate(post.getTitle() != null ? post.getTitle() : "Untitled", 110);
        String author = post.getAuthor() != null && post.getAuthor().getDisplayName() != null
            ? post.getAuthor().getDisplayName()
            : "Anonymous";
        String topic = post.getTopic() != null ? post.getTopic().getName() : null;
        String siteName = Optional.ofNullable(props.getSite().getTitle()).orElse("Personal Blog");

        return renderSvg(title, author, topic, siteName);
    }

    public static String renderGeneric(String title, String subtitle, BlogProperties props) {
        String siteName = Optional.ofNullable(props.getSite().getTitle()).orElse("Personal Blog");
        return renderSvg(truncate(title, 110), null, subtitle, siteName);
    }

    private static String renderSvg(String title, String author, String meta, String siteName) {
        // Escape XML special chars
        String safeTitle = escapeXml(title);
        String safeAuthor = author != null ? escapeXml(author) : null;
        String safeMeta = meta != null ? escapeXml(meta) : null;
        String safeSiteName = escapeXml(siteName);

        // Split title into lines for better display (max 60 chars per line)
        String[] titleLines = wrapText(safeTitle, 28);
        int titleStartY = 250;
        int lineHeight = 70;

        StringBuilder titleText = new StringBuilder();
        for (int i = 0; i < titleLines.length; i++) {
            int y = titleStartY + (i * lineHeight);
            titleText.append(String.format(
                "<text x=\"600\" y=\"%d\" text-anchor=\"middle\" " +
                "font-family=\"Georgia, serif\" font-size=\"56\" font-weight=\"700\" fill=\"#2d2d3a\">%s</text>%n",
                y, titleLines[i]));
        }

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
              <defs>
                <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                  <stop offset="0%%" stop-color="#faf6ee"/>
                  <stop offset="100%%" stop-color="#f0e9dc"/>
                </linearGradient>
              </defs>

              <!-- Background -->
              <rect width="%d" height="%d" fill="url(#bg)"/>

              <!-- Top brand bar -->
              <rect x="0" y="0" width="%d" height="100" fill="#2d2d3a"/>
              <text x="60" y="62" font-family="Inter, sans-serif" font-size="32" font-weight="700" fill="#f5f0ea">✦ %s</text>

              <!-- Decorative coral underline under brand -->
              <rect x="60" y="78" width="60" height="4" fill="#fb7185"/>

              <!-- Title -->
              %s

              <!-- Accent line below title -->
              <line x1="500" y1="450" x2="700" y2="450" stroke="#0d9488" stroke-width="4"/>

              <!-- Meta info (topic / subtitle) -->
              %s

              <!-- Author byline -->
              %s

              <!-- Bottom bar -->
              <rect x="0" y="%d" width="%d" height="50" fill="#2d2d3a"/>
              <text x="60" y="%d" font-family="Inter, sans-serif" font-size="18" fill="#f5f0ea" opacity="0.8">news.luuhailong.com</text>
            </svg>
            """.formatted(
                WIDTH, HEIGHT, WIDTH, HEIGHT,
                WIDTH, HEIGHT,
                WIDTH,
                safeSiteName,
                titleText,
                renderMeta(safeMeta),
                renderAuthor(safeAuthor),
                HEIGHT - 50, WIDTH,
                HEIGHT - 17
            );
    }

    private static String renderMeta(String meta) {
        if (meta == null || meta.isBlank()) return "";
        return String.format(
            "<text x=\"600\" y=\"510\" text-anchor=\"middle\" " +
            "font-family=\"Inter, sans-serif\" font-size=\"22\" fill=\"#57534e\">%s</text>%n",
            meta);
    }

    private static String renderAuthor(String author) {
        if (author == null || author.isBlank()) return "";
        return String.format(
            "<text x=\"600\" y=\"555\" text-anchor=\"middle\" " +
            "font-family=\"Inter, sans-serif\" font-size=\"20\" font-style=\"italic\" fill=\"#78716c\">— %s</text>%n",
            author);
    }

    // ── helpers ─────────────────────────────────────────────

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    /**
     * Word-wrap text into multiple lines for SVG.
     * Aims for ~28 chars per line.
     */
    private static String[] wrapText(String text, int approxCharsPerLine) {
        if (text == null || text.isEmpty()) return new String[]{""};
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String word : words) {
            if (current.length() + word.length() + 1 > approxCharsPerLine && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (current.length() > 0) current.append(' ');
                current.append(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        // Max 4 lines to fit in SVG
        if (lines.size() > 4) {
            String last = lines.get(3);
            lines = lines.subList(0, 4);
            lines.set(3, last.substring(0, Math.max(0, last.length() - 3)) + "...");
        }
        return lines.toArray(new String[0]);
    }
}
