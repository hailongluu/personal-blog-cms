package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.PostResponse;
import com.blog.cms.content.dto.ProjectResponse;
import com.blog.cms.content.dto.TopicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicPostService publicPostService;

    // ──────────────────────────────────────────────
    // Posts
    // ──────────────────────────────────────────────

    @GetMapping("/posts")
    public ApiResponse<List<PostResponse>> listPosts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return publicPostService.listPublishedPosts(type, tagId, page, size);
    }

    @GetMapping("/posts/featured")
    public ApiResponse<List<PostResponse>> listFeaturedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return publicPostService.listFeaturedPosts(page, size);
    }

    @GetMapping("/posts/{slug}")
    public ApiResponse<PostResponse> getPost(@PathVariable String slug) {
        return publicPostService.getPublishedPostBySlug(slug);
    }

    // ──────────────────────────────────────────────
    // Topics
    // ──────────────────────────────────────────────

    @GetMapping("/topics")
    public ApiResponse<List<TopicResponse>> listTopics() {
        return publicPostService.listTopics();
    }

    @GetMapping("/topics/{slug}")
    public ApiResponse<TopicResponse> getTopic(@PathVariable String slug) {
        return publicPostService.getTopicBySlug(slug);
    }

    @GetMapping("/topics/{slug}/posts")
    public ApiResponse<List<PostResponse>> getTopicPosts(
            @PathVariable String slug,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return publicPostService.getPostsByTopicSlug(slug, type, tagId, page, size);
    }

    // ──────────────────────────────────────────────
    // Projects
    // ──────────────────────────────────────────────

    @GetMapping("/projects")
    public ApiResponse<List<ProjectResponse>> listProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return publicPostService.listProjects(page, size);
    }

    @GetMapping("/projects/{slug}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable String slug) {
        return publicPostService.getProjectBySlug(slug);
    }

    // ──────────────────────────────────────────────
    // Newsletter
    // ──────────────────────────────────────────────

    @PostMapping("/newsletter")
    public ApiResponse<Void> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return publicPostService.subscribeNewsletter(email);
    }
}
