package com.blog.cms.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NewsletterSendResponse {
    private int recipientCount;
    private int successCount;
    private int failureCount;
    private String previewHtml; // null unless preview mode
}
