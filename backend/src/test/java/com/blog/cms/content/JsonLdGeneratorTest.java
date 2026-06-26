package com.blog.cms.content;

import com.blog.cms.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonLdGenerator — Schema.org BlogPosting")
class JsonLdGeneratorTest {

    @Test
    @DisplayName("null post returns null")
    void nullPost_returnsNull() {
        assertNull(JsonLdGenerator.forBlogPost(null));
    }

    @Test
    @DisplayName("Basic post generates valid JSON-LD with @context and @type")
    void basicPost_generatesValidJsonLd() {
        Post post = Post.builder()
            .id(1L)
            .title("Test Post Title")
            .slug("test-post")
            .contentMarkdown("This is a test post body.")
            .status("published")
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        assertNotNull(json);
        assertTrue(json.contains("\"@context\":\"https://schema.org\""));
        assertTrue(json.contains("\"@type\":\"BlogPosting\""));
        assertTrue(json.contains("\"headline\":\"Test Post Title\""));
    }

    @Test
    @DisplayName("Meta description takes priority over excerpt")
    void metaDescription_takesPriority() {
        Post post = Post.builder()
            .id(1L)
            .title("Title")
            .metaDescription("Meta description here")
            .excerpt("Excerpt here")
            .contentMarkdown("body")
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        assertTrue(json.contains("Meta description here"));
        assertFalse(json.contains("Excerpt here"));
    }

    @Test
    @DisplayName("OG image preferred over cover image")
    void ogImage_takesPriority() {
        Post post = Post.builder()
            .id(1L)
            .title("Title")
            .contentMarkdown("body")
            .ogImageUrl("https://example.com/og.png")
            .coverImageUrl("https://example.com/cover.png")
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        assertTrue(json.contains("og.png"));
        assertFalse(json.contains("cover.png"));
    }

    @Test
    @DisplayName("Headline is truncated at 110 chars")
    void headline_truncatedAt110() {
        String longTitle = "a".repeat(200);
        Post post = Post.builder()
            .id(1L)
            .title(longTitle)
            .contentMarkdown("body")
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        // Should be truncated to ~110 chars + "..."
        assertTrue(json.contains("..."));
        assertFalse(json.contains(longTitle));
    }

    @Test
    @DisplayName("Author is included as Person schema")
    void author_includedAsPerson() {
        User author = User.builder()
            .id(1L)
            .displayName("Lưu Hải Long")
            .build();
        Post post = Post.builder()
            .id(1L)
            .title("Title")
            .contentMarkdown("body")
            .author(author)
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        assertTrue(json.contains("\"author\""));
        assertTrue(json.contains("\"@type\":\"Person\""));
        assertTrue(json.contains("Lưu Hải Long"));
    }

    @Test
    @DisplayName("Dates are ISO 8601 formatted")
    void dates_isoFormatted() {
        Instant published = Instant.parse("2026-06-26T08:00:00Z");
        Instant updated = Instant.parse("2026-06-27T10:00:00Z");

        Post post = Post.builder()
            .id(1L)
            .title("Title")
            .contentMarkdown("body")
            .publishedAt(published)
            .updatedAt(updated)
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        assertTrue(json.contains("datePublished"));
        assertTrue(json.contains("dateModified"));
        assertTrue(json.contains("2026-06-26"));
    }

    @Test
    @DisplayName("Tags are joined as comma-separated keywords")
    void tags_joinedAsKeywords() {
        Tag tag1 = Tag.builder().id(1L).name("AI").slug("ai").build();
        Tag tag2 = Tag.builder().id(2L).name("Data").slug("data").build();
        Set<Tag> tags = new HashSet<>();
        tags.add(tag1);
        tags.add(tag2);

        Post post = Post.builder()
            .id(1L)
            .title("Title")
            .contentMarkdown("body")
            .tags(tags)
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        // Order of Set is not guaranteed — check both names present
        assertTrue(json.contains("AI"));
        assertTrue(json.contains("Data"));
        assertTrue(json.contains("\"keywords\""));
    }

    @Test
    @DisplayName("Word count is calculated from markdown")
    void wordCount_calculatedFromMarkdown() {
        Post post = Post.builder()
            .id(1L)
            .title("Title")
            .contentMarkdown("one two three four five")
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        assertTrue(json.contains("\"wordCount\":5"));
    }

    @Test
    @DisplayName("JSON-LD is properly escaped (no unescaped quotes)")
    void jsonLd_isEscaped() {
        Post post = Post.builder()
            .id(1L)
            .title("Title with \"quotes\"")
            .contentMarkdown("body")
            .build();

        String json = JsonLdGenerator.forBlogPost(post);

        // Quotes should be escaped
        assertTrue(json.contains("\\\"quotes\\\""));
    }
}
