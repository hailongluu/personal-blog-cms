package com.blog.cms.content;

import com.blog.cms.content.dto.PostResponse;
import com.blog.cms.config.BlogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RSS / Atom feed for published blog posts.
 * Returns Atom 1.0 (XML) — modern, well-supported, includes author + summary.
 *
 * Spec: https://datatracker.ietf.org/doc/html/rfc4287
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class RssFeedController {

    private final PublicPostService publicPostService;
    private final BlogProperties blogProperties;

    private static final int FEED_LIMIT = 20;

    @GetMapping(value = "/feed.xml", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    public ResponseEntity<String> getFeed() {
        List<PostResponse> posts = publicPostService.listPublishedPosts(null, null, 1, FEED_LIMIT).getData();
        String xml = renderAtom(posts);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_ATOM_XML)
                .body(xml);
    }

    private String renderAtom(List<PostResponse> posts) {
        String baseUrl = blogProperties.getSite().getUrl();
        String updated = posts.isEmpty()
                ? java.time.Instant.now().toString()
                : posts.get(0).getPublishedAt().toString();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <title>").append(escape(blogProperties.getSite().getTitle())).append("</title>\n");
        xml.append("  <subtitle>").append(escape(blogProperties.getSite().getDescription())).append("</subtitle>\n");
        xml.append("  <link href=\"").append(baseUrl).append("/blog\" rel=\"alternate\"/>\n");
        xml.append("  <link href=\"").append(baseUrl).append("/api/public/feed.xml\" rel=\"self\"/>\n");
        xml.append("  <id>").append(baseUrl).append("/</id>\n");
        xml.append("  <updated>").append(updated).append("</updated>\n");

        for (PostResponse post : posts) {
            String postUrl = baseUrl + "/blog/" + post.getSlug();
            xml.append("  <entry>\n");
            xml.append("    <title>").append(escape(post.getTitle())).append("</title>\n");
            xml.append("    <link href=\"").append(postUrl).append("\"/>\n");
            xml.append("    <id>").append(postUrl).append("</id>\n");
            if (post.getPublishedAt() != null) {
                xml.append("    <published>").append(post.getPublishedAt()).append("</published>\n");
                xml.append("    <updated>").append(post.getPublishedAt()).append("</updated>\n");
            }
            if (post.getExcerpt() != null && !post.getExcerpt().isBlank()) {
                xml.append("    <summary type=\"text\">").append(escape(post.getExcerpt())).append("</summary>\n");
            }
            if (post.getAuthor() != null && post.getAuthor().getDisplayName() != null) {
                xml.append("    <author><name>").append(escape(post.getAuthor().getDisplayName())).append("</name></author>\n");
            }
            xml.append("  </entry>\n");
        }

        xml.append("</feed>\n");
        return xml.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
