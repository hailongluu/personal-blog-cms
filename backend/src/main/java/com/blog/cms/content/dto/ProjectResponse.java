package com.blog.cms.content.dto;

import com.blog.cms.content.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String contentMarkdown;
    private String coverImageUrl;
    private String projectUrl;
    private String repoUrl;
    private List<String> techStack;
    private String status;
    private Boolean isFeatured;
    private Integer sortOrder;
    private LocalDate startedAt;
    private LocalDate completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProjectResponse from(Project p) {
        return ProjectResponse.builder()
            .id(p.getId())
            .title(p.getTitle())
            .slug(p.getSlug())
            .description(p.getDescription())
            .contentMarkdown(p.getContentMarkdown())
            .coverImageUrl(p.getCoverImageUrl())
            .projectUrl(p.getProjectUrl())
            .repoUrl(p.getRepoUrl())
            .techStack(p.getTechStack())
            .status(p.getStatus())
            .isFeatured(p.getIsFeatured())
            .sortOrder(p.getSortOrder())
            .startedAt(p.getStartedAt())
            .completedAt(p.getCompletedAt())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}
