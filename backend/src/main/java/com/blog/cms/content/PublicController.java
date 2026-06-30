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

    @GetMapping("/posts/{slug}/related")
    public ApiResponse<List<PostResponse>> getRelatedPosts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "3") int limit) {
        return publicPostService.getRelatedPosts(slug, limit);
    }

    @GetMapping("/search")
    public ApiResponse<List<PostResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return publicPostService.searchPublished(q, page, size);
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

    @GetMapping("/tags/{slug}/posts")
    public ApiResponse<List<PostResponse>> getTagPosts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return publicPostService.getPostsByTagSlug(slug, page, size);
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

    @GetMapping("/newsletter/confirm")
    public ApiResponse<Void> confirmNewsletter(@RequestParam(required = false) String token) {
        return publicPostService.confirmNewsletter(token);
    }

    @GetMapping("/newsletter/unsubscribe")
    public ApiResponse<Void> unsubscribeNewsletter(@RequestParam(required = false) String email) {
        return publicPostService.unsubscribeNewsletter(email);
    }
}
