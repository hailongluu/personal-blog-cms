package com.blog.cms.content;

import com.blog.cms.config.BlogProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgentReadyController {

    private final PublicPostService publicPostService;
    private final BlogProperties blogProperties;

    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> agentJson(HttpServletRequest request) {
        String baseUrl = resolveBaseUrl(request);
        String title = blogProperties.getSite().getTitle();
        String description = blogProperties.getSite().getDescription();

        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("title", title);
        agent.put("description", description);
        agent.put("type", "blog");

        var endpoints = new LinkedHashMap<String, Object>();
        Map<String, String> postsList = new LinkedHashMap<>();
        postsList.put("url", baseUrl + "/api/public/posts");
        postsList.put("method", "GET");
        postsList.put("description", "Danh sách bài viết đã xuất bản (có phân trang)");

        Map<String, String> postsDetail = new LinkedHashMap<>();
        postsDetail.put("url", baseUrl + "/api/public/posts/{slug}");
        postsDetail.put("method", "GET");
        postsDetail.put("description", "Chi tiết bài viết");

        Map<String, String> postsMd = new LinkedHashMap<>();
        postsMd.put("url", baseUrl + "/api/public/posts/{slug}.md");
        postsMd.put("method", "GET");
        postsMd.put("description", "Nội dung markdown thô của bài viết");

        Map<String, String> topicsList = new LinkedHashMap<>();
        topicsList.put("url", baseUrl + "/api/public/topics");
        topicsList.put("method", "GET");
        topicsList.put("description", "Danh sách chủ đề");

        Map<String, String> topicsDetail = new LinkedHashMap<>();
        topicsDetail.put("url", baseUrl + "/api/public/topics/{slug}");
        topicsDetail.put("method", "GET");
        topicsDetail.put("description", "Chi tiết chủ đề + danh sách bài viết");

        Map<String, String> projectsList = new LinkedHashMap<>();
        projectsList.put("url", baseUrl + "/api/public/projects");
        projectsList.put("method", "GET");
        projectsList.put("description", "Danh sách dự án");

        Map<String, String> projectsDetail = new LinkedHashMap<>();
        projectsDetail.put("url", baseUrl + "/api/public/projects/{slug}");
        projectsDetail.put("method", "GET");
        projectsDetail.put("description", "Chi tiết dự án");

        Map<String, String> newsletter = new LinkedHashMap<>();
        newsletter.put("url", baseUrl + "/api/public/newsletter");
        newsletter.put("method", "POST");
        newsletter.put("description", "Đăng ký nhận bản tin (email)");

        Map<String, String> sitemap = new LinkedHashMap<>();
        sitemap.put("url", baseUrl + "/sitemap.xml");
        sitemap.put("method", "GET");
        sitemap.put("description", "Sitemap XML");

        Map<String, String> robots = new LinkedHashMap<>();
        robots.put("url", baseUrl + "/robots.txt");
        robots.put("method", "GET");
        robots.put("description", "Robots.txt cho SEO");

        endpoints.put("posts.list", postsList);
        endpoints.put("posts.detail", postsDetail);
        endpoints.put("posts.markdown", postsMd);
        endpoints.put("topics.list", topicsList);
        endpoints.put("topics.detail", topicsDetail);
        endpoints.put("projects.list", projectsList);
        endpoints.put("projects.detail", projectsDetail);
        endpoints.put("newsletter.subscribe", newsletter);
        endpoints.put("seo.sitemap", sitemap);
        endpoints.put("seo.robots", robots);

        agent.put("endpoints", endpoints);
        agent.put("version", "1.0");

        return agent;
    }

    @GetMapping(value = "/api/public/posts/{slug}.md", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> getPostMarkdown(@PathVariable String slug) {
        String markdown = publicPostService.getPublishedPostMarkdown(slug);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(markdown);
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String configured = blogProperties.getSite().getUrl();
        if (configured != null && !configured.isBlank() && !"http://localhost:8080".equals(configured)) {
            return configured.replaceAll("/$", "");
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
