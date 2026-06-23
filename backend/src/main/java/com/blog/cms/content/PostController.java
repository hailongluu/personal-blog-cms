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

    @GetMapping("/posts")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<List<PostResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "updated_at") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.list(status, topicId, authorId, search, sort, page, size);
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
