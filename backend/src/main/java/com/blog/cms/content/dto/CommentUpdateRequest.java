package com.blog.cms.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommentUpdateRequest {
    private String content;
    /** Required: only the original author may edit within the edit window. */
    private String authorEmail;
}
