package com.blog.cms.content.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdatePostRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String subtitle;

    private String excerpt;

    private String contentMarkdown;

    private String contentHtml;

    private String coverImageUrl;

    private String ogImageUrl;

    @Size(max = 20)
    private String status;

    @Size(max = 20)
    private String visibility;

    @Size(max = 30)
    private String type;

    private Boolean featured;

    private Long topicId;

    private Set<Long> tagIds;

    // --- SEO ---
    @Size(max = 255)
    private String metaTitle;

    @Size(max = 500)
    private String metaDescription;

    private String canonicalUrl;

    private Instant scheduledAt;
}
