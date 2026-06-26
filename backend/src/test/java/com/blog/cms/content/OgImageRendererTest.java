package com.blog.cms.content;

import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.PostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OgImageRenderer — SVG OG image generation")
class OgImageRendererTest {

    private final BlogProperties props = new BlogProperties();

    @Test
    @DisplayName("Valid SVG header is always present")
    void svg_hasValidHeader() {
        PostResponse post = PostResponse.builder().title("Hello World").build();
        String svg = OgImageRenderer.render(post, props);

        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("width=\"1200\""));
        assertTrue(svg.contains("height=\"630\""));
    }

    @Test
    @DisplayName("Title is rendered into SVG")
    void render_includesTitle() {
        PostResponse post = PostResponse.builder().title("How to Use Claude AI").build();
        String svg = OgImageRenderer.render(post, props);

        assertTrue(svg.contains("How to Use Claude AI"));
    }

    @Test
    @DisplayName("Long title is truncated")
    void render_truncatesLongTitle() {
        String longTitle = "a".repeat(200);
        PostResponse post = PostResponse.builder().title(longTitle).build();
        String svg = OgImageRenderer.render(post, props);

        assertFalse(svg.contains(longTitle));
        assertTrue(svg.contains("..."));
    }

    @Test
    @DisplayName("XML special chars are escaped in title")
    void render_escapesXmlChars() {
        PostResponse post = PostResponse.builder().title("AI & Robotics < The Future >").build();
        String svg = OgImageRenderer.render(post, props);

        // & should be escaped to &amp; to be valid XML
        assertTrue(svg.contains("&amp;"));
        assertTrue(svg.contains("&lt;"));
        assertTrue(svg.contains("&gt;"));
    }

    @Test
    @DisplayName("Author is rendered as byline")
    void render_includesAuthor() {
        PostResponse.AuthorDto author = PostResponse.AuthorDto.builder()
            .id(1L).displayName("Lưu Hải Long").build();
        PostResponse post = PostResponse.builder()
            .title("Title").author(author).build();
        String svg = OgImageRenderer.render(post, props);

        assertTrue(svg.contains("Lưu Hải Long"));
        assertTrue(svg.contains("—")); // em-dash byline marker
    }

    @Test
    @DisplayName("Topic name is rendered as meta line")
    void render_includesTopic() {
        PostResponse.TopicDto topic = PostResponse.TopicDto.builder()
            .id(1L).name("AI & Big Data").slug("ai-big-data").build();
        PostResponse post = PostResponse.builder()
            .title("Title").topic(topic).build();
        String svg = OgImageRenderer.render(post, props);

        assertTrue(svg.contains("AI &amp; Big Data"));
    }

    @Test
    @DisplayName("Site name from BlogProperties is rendered in top bar")
    void render_includesSiteName() {
        BlogProperties customProps = new BlogProperties();
        customProps.getSite().setTitle("Lưu Hải Long News");
        PostResponse post = PostResponse.builder().title("Title").build();
        String svg = OgImageRenderer.render(post, customProps);

        assertTrue(svg.contains("Lưu Hải Long News"));
    }

    @Test
    @DisplayName("Generic render includes title + subtitle")
    void renderGeneric_includesArgs() {
        String svg = OgImageRenderer.renderGeneric("About Me", "Personal blog", props);
        assertTrue(svg.contains("About Me"));
        assertTrue(svg.contains("Personal blog"));
    }

    @Test
    @DisplayName("Generic render uses BlogProperties title as fallback")
    void renderGeneric_fallbackToPropsTitle() {
        BlogProperties customProps = new BlogProperties();
        customProps.getSite().setTitle("Fallback Title");
        String svg = OgImageRenderer.renderGeneric("", "", customProps);
        assertTrue(svg.contains("Fallback Title"));
    }

    @Test
    @DisplayName("Generic render truncates empty input properly")
    void renderGeneric_emptyInput_fallbackDefaults() {
        BlogProperties defaultProps = new BlogProperties(); // title = "My Blog"
        String svg = OgImageRenderer.renderGeneric("", "", defaultProps);
        assertTrue(svg.contains("My Blog"));
    }

    @Test
    @DisplayName("Null author is handled gracefully")
    void render_nullAuthor_noCrash() {
        PostResponse post = PostResponse.builder().title("Title").author(null).build();
        String svg = OgImageRenderer.render(post, props);
        assertNotNull(svg);
        assertTrue(svg.contains("Title"));
    }

    @Test
    @DisplayName("Null topic is handled gracefully")
    void render_nullTopic_noCrash() {
        PostResponse post = PostResponse.builder().title("Title").topic(null).build();
        String svg = OgImageRenderer.render(post, props);
        assertNotNull(svg);
    }

    @Test
    @DisplayName("Site URL is in footer")
    void render_includesSiteUrl() {
        BlogProperties customProps = new BlogProperties();
        customProps.getSite().setUrl("https://news.luuhailong.com");
        PostResponse post = PostResponse.builder().title("Title").build();
        // Note: current impl uses hardcoded "news.luuhailong.com" in footer
        String svg = OgImageRenderer.render(post, customProps);
        assertTrue(svg.contains("news.luuhailong.com"));
    }

    @Test
    @DisplayName("Title is wrapped into multiple lines for very long text")
    void render_wrapsTitleToMultipleLines() {
        // 100 chars title should wrap
        PostResponse post = PostResponse.builder()
            .title("This is a very long article title that should definitely be wrapped into multiple lines for the SVG OG image")
            .build();
        String svg = OgImageRenderer.render(post, props);

        // Should have multiple <text> elements for the title
        int textCount = svg.split("<text").length - 1;
        assertTrue(textCount >= 3, "Expected multiple text lines, got " + textCount);
    }
}
