package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CommentCreateRequest;
import com.blog.cms.content.dto.CommentResponse;
import com.blog.cms.content.dto.CommentUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public comment endpoints — anyone can submit, only approved comments are
 * visible, authors may edit their own comment within a 5-minute window.
 */
@Slf4j
@RestController
@RequestMapping("/api/public/comments")
@RequiredArgsConstructor
public class CommentPublicController {

    private final CommentService commentService;

    /** List approved comments for a post (flat, ordered by createdAt asc). */
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> listForPost(
            @PathVariable Long postId) {
        return ResponseEntity.ok(commentService.listApprovedForPost(postId));
    }

    /** Submit a new comment. New comments enter the moderation queue (status=pending). */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @RequestBody CommentCreateRequest req,
            HttpServletRequest httpReq) {
        try {
            ApiResponse<CommentResponse> resp = commentService.create(req);
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.info("Comment create rejected: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    /** Author edits own comment within 5-minute window (verified by authorEmail). */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable Long id,
            @RequestBody CommentUpdateRequest req) {
        try {
            return ResponseEntity.ok(commentService.update(id, req));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}
