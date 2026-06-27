package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CommentCreateRequest;
import com.blog.cms.content.dto.CommentResponse;
import com.blog.cms.content.dto.CommentUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Comment business logic:
 *   - public create / list approved
 *   - admin moderation (approve / reject / spam)
 *   - author self-edit within a 5-minute window
 *   - soft delete
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Duration EDIT_WINDOW = Duration.ofMinutes(5);

    private static final Set<String> MODERATION_STATUSES =
            Set.of("pending", "approved", "rejected", "spam", "deleted");

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final SettingsRepository settingsRepository;

    // ─────────── public create ───────────

    @Transactional
    public ApiResponse<CommentResponse> create(CommentCreateRequest req) {
        validateCreate(req);

        if (!commentsEnabled()) {
            throw new IllegalStateException("comments are disabled for this site");
        }

        Post post = postRepository.findById(req.getPostId())
                .orElseThrow(() -> new EntityNotFoundException("Resource not found"));
        if (!"published".equals(post.getStatus())) {
            throw new IllegalStateException("post is not published");
        }

        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("parent comment not found"));
            if (!parent.getPostId().equals(req.getPostId())) {
                throw new IllegalArgumentException("parent comment belongs to different post");
            }
        }

        Comment c = Comment.builder()
                .postId(req.getPostId())
                .parentId(req.getParentId())
                .authorName(req.getAuthorName().trim())
                .authorEmail(req.getAuthorEmail().trim().toLowerCase())
                .content(req.getContent().trim())
                .status("pending")
                .build();

        Comment saved = commentRepository.save(c);
        log.info("Comment created: id={} post={} parent={} author={}",
                saved.getId(), saved.getPostId(), saved.getParentId(), saved.getAuthorName());
        return ApiResponse.ok(CommentResponse.from(saved, false));
    }

    // ─────────── public list approved ───────────

    @Transactional(readOnly = true)
    public ApiResponse<List<CommentResponse>> listApprovedForPost(Long postId) {
        List<Comment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(postId, "approved");
        List<CommentResponse> out = comments.stream()
                .map(c -> CommentResponse.from(c, false))
                .toList();
        return ApiResponse.ok(out);
    }

    // ─────────── moderation ───────────

    @Transactional
    public ApiResponse<CommentResponse> moderate(Long commentId, String newStatus) {
        if (!MODERATION_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "invalid status — must be one of " + MODERATION_STATUSES);
        }
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("comment not found"));
        c.setStatus(newStatus);
        c.setModeratedAt(Instant.now());
        Comment saved = commentRepository.save(c);
        log.info("Comment moderated: id={} status={}", commentId, newStatus);
        return ApiResponse.ok(CommentResponse.from(saved, false));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<CommentResponse>> listModerationQueue() {
        List<Comment> pending = commentRepository
                .findByStatusOrderByCreatedAtAsc("pending");
        List<CommentResponse> out = pending.stream()
                .map(c -> CommentResponse.from(c, false))
                .toList();
        return ApiResponse.ok(out);
    }

    // ─────────── author self-edit ───────────

    @Transactional
    public ApiResponse<CommentResponse> update(Long commentId, CommentUpdateRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("comment not found"));

        if (req.getAuthorEmail() == null
                || c.getAuthorEmail() == null
                || !c.getAuthorEmail().equalsIgnoreCase(req.getAuthorEmail().trim())) {
            throw new IllegalStateException("only the original author may edit");
        }

        if (c.getCreatedAt() != null
                && Duration.between(c.getCreatedAt(), Instant.now()).compareTo(EDIT_WINDOW) > 0) {
            throw new IllegalStateException(
                    "edit window of " + EDIT_WINDOW.toMinutes() + " minutes has passed");
        }

        c.setContent(req.getContent().trim());
        c.setUpdatedAt(Instant.now());
        Comment saved = commentRepository.save(c);
        return ApiResponse.ok(CommentResponse.from(saved, false));
    }

    // ─────────── delete ───────────

    @Transactional
    public ApiResponse<Void> delete(Long commentId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("comment not found"));
        c.setStatus("deleted");
        c.setDeletedAt(Instant.now());
        commentRepository.save(c);
        log.info("Comment soft-deleted: id={}", commentId);
        return ApiResponse.ok(null);
    }

    // ─────────── helpers ───────────

    private void validateCreate(CommentCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("request is null");
        if (req.getPostId() == null) throw new IllegalArgumentException("postId is required");
        if (req.getAuthorName() == null || req.getAuthorName().isBlank()) {
            throw new IllegalArgumentException("authorName is required");
        }
        if (req.getAuthorEmail() == null
                || !req.getAuthorEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("authorEmail is invalid");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    private boolean commentsEnabled() {
        return settingsRepository.findById("posts.allow_comments")
                .map(s -> {
                    String v = s.getValue();
                    return v != null && (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equals("1"));
                })
                .orElse(true); // default ON if setting missing
    }
}
