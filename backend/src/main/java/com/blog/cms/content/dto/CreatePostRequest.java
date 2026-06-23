package com.blog.cms.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    /**
     * Auto-generated from title when blank.
     * Must match SPEC §6.4 slug regex when provided.
     */
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Slug must be lowercase alphanumeric with hyphens")
    @Size(max = 280)
    private String slug;

    @Size(max = 1000)
    private String subtitle;

    @Size(max = 500)
    private String excerpt;

    private String contentMarkdown;

    private String contentHtml;

    private String coverImageUrl;

    private String ogImageUrl;

    /**
     * Post lifecycle — SPEC §6.2: draft|reviewing|published|archived
     */
    @Size(max = 20)
    private String status = "draft";

    @Size(max = 20)
    private String visibility = "public";

    /**
     * Post type — SPEC §8.3.4: ESSAY|RESEARCH_BRIEF|FIELD_NOTE|BUILD_LOG|PLAYBOOK|REVIEW|PERSONAL_LOG
     */
    @Size(max = 30)
    private String type = "essay";

    private boolean featured = false;

    private Long topicId;

    private Set<Long> tagIds;

    // --- SEO ---
    @Size(max = 255)
    private String metaTitle;

    @Size(max = 500)
    private String metaDescription;

    private String canonicalUrl;
}
