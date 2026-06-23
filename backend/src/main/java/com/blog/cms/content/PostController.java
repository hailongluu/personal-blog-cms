package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.*;
import com.blog.cms.security.BlogUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * GET /api/admin/posts — filterable list
     * Query params (SPEC §8.3.11): page, pageSize, q, status, type,
     *   topicId, tagId, featured, sort, direction
     */
    @GetMapping("/posts")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<List<PostResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "updated_at") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Support both ?q= and ?search= for frontend flexibility
        String query = q != null ? q : search;
        return postService.list(status, type, topicId, authorId, tagId, featured,
            query, sort, page, size);
    }

    @GetMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<PostResponse> getById(@PathVariable Long id) {
        return postService.findById(id);
    }

    @GetMapping("/posts/slug/{slug}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<PostResponse> getBySlug(@PathVariable String slug) {
        return postService.findBySlug(slug);
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> create(
            @Valid @RequestBody CreatePostRequest req,
            @AuthenticationPrincipal BlogUserDetails user) {
        return postService.create(req, user.getUsername());
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<PostResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest req) {
        return postService.update(id, req);
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return postService.softDelete(id);
    }

    // ───────────────────────────────────────────────────────────
    // Lifecycle endpoints — SPEC §8.3.10
    // ───────────────────────────────────────────────────────────

    @PostMapping("/posts/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<PostResponse> publish(@PathVariable Long id) {
        return postService.publish(id);
    }

    @PostMapping("/posts/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<PostResponse> unpublish(@PathVariable Long id) {
        return postService.unpublish(id);
    }

    @PostMapping("/posts/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<PostResponse> archive(@PathVariable Long id) {
        return postService.archive(id);
    }

    @PostMapping("/posts/{id}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<PostResponse> duplicate(
            @PathVariable Long id,
            @AuthenticationPrincipal BlogUserDetails user) {
        return postService.duplicate(id, user.getUsername());
    }

    @GetMapping("/posts/{id}/preview")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<PostResponse> preview(@PathVariable Long id) {
        return postService.preview(id);
    }

    // ───────────────────────────────────────────────────────────
    // Soft delete management
    // ───────────────────────────────────────────────────────────

    @PostMapping("/posts/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        return postService.restore(id);
    }

    @GetMapping("/posts/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PostResponse>> listDeleted(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.listDeleted(page, size);
    }
}
