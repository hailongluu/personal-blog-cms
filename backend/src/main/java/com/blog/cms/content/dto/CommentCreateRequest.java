package com.blog.cms.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommentCreateRequest {
    private Long postId;
    private Long parentId; // null for top-level
    private String authorName;
    private String authorEmail;
    private String content;
}
