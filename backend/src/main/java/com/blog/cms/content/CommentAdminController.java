package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin moderation queue for comments.
 *
 *   GET   /api/admin/comments/moderation     → list pending
 *   POST  /api/admin/comments/{id}/moderate  → approve | reject | spam
 *   DELETE /api/admin/comments/{id}          → soft-delete
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
@RequiredArgsConstructor
public class CommentAdminController {

    private final CommentService commentService;

    @GetMapping("/moderation")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> moderationQueue() {
        return ResponseEntity.ok(commentService.listModerationQueue());
    }

    @PostMapping("/{id}/moderate")
    public ResponseEntity<ApiResponse<CommentResponse>> moderate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        try {
            return ResponseEntity.ok(commentService.moderate(id, status));
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commentService.delete(id));
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
