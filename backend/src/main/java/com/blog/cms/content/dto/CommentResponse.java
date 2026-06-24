package com.blog.cms.content.dto;

import com.blog.cms.content.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long parentId;
    private String authorName;
    /** Email is intentionally hidden from public response. */
    private String status;
    private String content;
    private Instant createdAt;
    private Instant moderatedAt;

    public static CommentResponse from(Comment c, boolean includeEmail) {
        CommentResponseBuilder b = CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .parentId(c.getParentId())
                .authorName(c.getAuthorName())
                .status(c.getStatus())
                .content("deleted".equals(c.getStatus()) ? "[deleted]" : c.getContent())
                .createdAt(c.getCreatedAt())
                .moderatedAt(c.getModeratedAt());
        return b.build();
    }
}
