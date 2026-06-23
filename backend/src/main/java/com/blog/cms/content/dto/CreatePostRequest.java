package com.blog.cms.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreatePostRequest {

    @NotBlank @Size(max = 255)
    private String title;

    @Size(max = 280)
    private String slug; // auto-generated if blank

    @Size(max = 500)
    private String excerpt;

    private String contentMarkdown;

    private String contentHtml;

    private String coverImageUrl;

    private String ogImageUrl;

    @Size(max = 20)
    private String status = "draft";

    @Size(max = 20)
    private String visibility = "public";

    private Long topicId;

    private Set<Long> tagIds;

    // --- SEO ---
    @Size(max = 255)
    private String metaTitle;

    @Size(max = 500)
    private String metaDescription;

    private String canonicalUrl;
}
