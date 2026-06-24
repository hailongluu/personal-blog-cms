package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test for RssFeedController.
 * - Valid Atom 1.0 XML structure
 * - Correct content type
 * - Renders posts as <entry> elements
 */
@ExtendWith(MockitoExtension.class)
class RssFeedControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PublicPostService publicPostService;

    private BlogProperties blogProperties;

    @InjectMocks
    private RssFeedController controller;

    @BeforeEach
    void setup() {
        blogProperties = new BlogProperties();
        blogProperties.getSite().setTitle("Test Blog");
        blogProperties.getSite().setDescription("A test blog");
        blogProperties.getSite().setUrl("https://test.example.com");
        controller = new RssFeedController(publicPostService, blogProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void feed_returnsAtomXmlWith200() throws Exception {
        when(publicPostService.listPublishedPosts(eq(null), eq(null), eq(1), eq(20)))
                .thenReturn(ApiResponse.<List<PostResponse>>builder().data(List.of()).build());

        mockMvc.perform(get("/api/public/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML));
    }

    @Test
    void feed_containsValidXmlDeclaration() throws Exception {
        when(publicPostService.listPublishedPosts(eq(null), eq(null), eq(1), eq(20)))
                .thenReturn(ApiResponse.<List<PostResponse>>builder().data(List.of()).build());

        mockMvc.perform(get("/api/public/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("<?xml")));
    }

    @Test
    void feed_hasFeedRootElement() throws Exception {
        when(publicPostService.listPublishedPosts(eq(null), eq(null), eq(1), eq(20)))
                .thenReturn(ApiResponse.<List<PostResponse>>builder().data(List.of()).build());

        mockMvc.perform(get("/api/public/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<feed")));
    }

    @Test
    void feed_rendersPostAsEntry() throws Exception {
        PostResponse post = PostResponse.builder()
                .id(1L)
                .title("My Post")
                .slug("my-post")
                .excerpt("Post summary")
                .publishedAt(Instant.parse("2026-06-24T00:00:00Z"))
                .author(PostResponse.AuthorDto.builder().id(1L).displayName("Author").build())
                .build();
        when(publicPostService.listPublishedPosts(any(), any(), eq(1), eq(20)))
                .thenReturn(ApiResponse.<List<PostResponse>>builder().data(List.of(post)).build());

        mockMvc.perform(get("/api/public/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<entry>")))
                .andExpect(content().string(containsString("My Post")))
                .andExpect(content().string(containsString("/blog/my-post")))
                .andExpect(content().string(containsString("Post summary")));
    }
}
