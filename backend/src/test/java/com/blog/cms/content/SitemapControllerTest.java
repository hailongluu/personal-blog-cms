package com.blog.cms.content;

import com.blog.cms.content.PublicPostService;
import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.PostResponse;
import com.blog.cms.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SitemapControllerTest {

    private MockMvc mockMvc;

    @Mock private PublicPostService publicPostService;
    @Mock private TopicRepository topicRepository;
    @Mock private ProjectRepository projectRepository;

    private BlogProperties blogProperties;

    @BeforeEach
    void setUp() {
        blogProperties = new BlogProperties();
        blogProperties.getSite().setUrl("https://task.luuhailong.com");
        SitemapController controller = new SitemapController(
                publicPostService, topicRepository, projectRepository, blogProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void sitemap_returnsXmlContentType() throws Exception {
        when(publicPostService.listPublishedPosts(null, null, 1, 1000))
                .thenReturn(ApiResponse.ok(List.of()));
        when(topicRepository.findAllActive()).thenReturn(List.of());
        when(projectRepository.findAllActive(org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/public/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    void sitemap_containsStaticRoutes() throws Exception {
        when(publicPostService.listPublishedPosts(null, null, 1, 1000))
                .thenReturn(ApiResponse.ok(List.of()));
        when(topicRepository.findAllActive()).thenReturn(List.of());
        when(projectRepository.findAllActive(org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/public/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://task.luuhailong.com/")))
                .andExpect(content().string(containsString("https://task.luuhailong.com/blog")))
                .andExpect(content().string(containsString("https://task.luuhailong.com/about")))
                .andExpect(content().string(containsString("https://task.luuhailong.com/newsletter")));
    }

    @Test
    void sitemap_includesPublishedPosts() throws Exception {
        PostResponse post1 = PostResponse.builder()
                .slug("qwen-code-la-gi")
                .title("Qwen Code là gì?")
                .updatedAt(Instant.parse("2026-06-20T10:00:00Z"))
                .build();
        PostResponse post2 = PostResponse.builder()
                .slug("krea-2-la-gi")
                .title("Krea 2 là gì?")
                .updatedAt(Instant.parse("2026-06-22T10:00:00Z"))
                .build();

        when(publicPostService.listPublishedPosts(null, null, 1, 1000))
                .thenReturn(ApiResponse.ok(List.of(post1, post2)));
        when(topicRepository.findAllActive()).thenReturn(List.of());
        when(projectRepository.findAllActive(org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/public/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://task.luuhailong.com/blog/qwen-code-la-gi")))
                .andExpect(content().string(containsString("https://task.luuhailong.com/blog/krea-2-la-gi")))
                .andExpect(content().string(containsString("<lastmod>2026-06-20")))
                .andExpect(content().string(containsString("<lastmod>2026-06-22")));
    }

    @Test
    void sitemap_includesTopicsAndProjects() throws Exception {
        Topic topic = new Topic();
        topic.setSlug("ai-agents");
        topic.setUpdatedAt(Instant.parse("2026-06-15T10:00:00Z"));

        Project project = new Project();
        project.setSlug("tosea-articles");
        project.setUpdatedAt(Instant.parse("2026-06-10T10:00:00Z"));

        when(publicPostService.listPublishedPosts(null, null, 1, 1000))
                .thenReturn(ApiResponse.ok(List.of()));
        when(topicRepository.findAllActive()).thenReturn(List.of(topic));
        when(projectRepository.findAllActive(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(project)));

        mockMvc.perform(get("/api/public/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://task.luuhailong.com/topic/ai-agents")))
                .andExpect(content().string(containsString("https://task.luuhailong.com/project/tosea-articles")));
    }

    @Test
    void sitemap_isValidXml() throws Exception {
        when(publicPostService.listPublishedPosts(null, null, 1, 1000))
                .thenReturn(ApiResponse.ok(List.of()));
        when(topicRepository.findAllActive()).thenReturn(List.of());
        when(projectRepository.findAllActive(org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        String xml = mockMvc.perform(get("/api/public/sitemap.xml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Must contain xml declaration + urlset root
        org.junit.jupiter.api.Assertions.assertTrue(xml.startsWith("<?xml"), "Must start with XML declaration");
        org.junit.jupiter.api.Assertions.assertTrue(xml.contains("<urlset"), "Must contain <urlset>");
        org.junit.jupiter.api.Assertions.assertTrue(xml.contains("</urlset>"), "Must close </urlset>");
        org.junit.jupiter.api.Assertions.assertTrue(xml.contains("<loc>"), "Must have <loc> entries");
        org.junit.jupiter.api.Assertions.assertTrue(xml.contains("<changefreq>"), "Must have changefreq");
    }
}
