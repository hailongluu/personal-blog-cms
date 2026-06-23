package com.blog.cms.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectRequest {

    @NotBlank @Size(max = 255)
    private String title;

    @Size(max = 280)
    private String slug;

    @Size(max = 2000)
    private String description;

    private String contentMarkdown;

    private String coverImageUrl;

    private String projectUrl;

    private String repoUrl;

    private List<String> techStack;

    @Size(max = 20)
    private String status = "in_progress";

    private Boolean isFeatured;

    private Integer sortOrder;

    private LocalDate startedAt;

    private LocalDate completedAt;
}
