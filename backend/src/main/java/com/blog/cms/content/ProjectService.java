package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.ProjectRequest;
import com.blog.cms.content.dto.ProjectResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public ApiResponse<List<ProjectResponse>> list(int page, int size) {
        Page<Project> projects = projectRepository.findAllActive(PageRequest.of(page - 1, size));
        var data = projects.getContent().stream().map(ProjectResponse::from).toList();
        return ApiResponse.paged(data, page, size, projects.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApiResponse<ProjectResponse> findById(Long id) {
        Project project = projectRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
        return ApiResponse.ok(ProjectResponse.from(project));
    }

    public ApiResponse<ProjectResponse> create(ProjectRequest req) {
        String slug = req.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.slugify(req.getTitle());
        }
        int counter = 1;
        String base = slug;
        while (projectRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }

        Project project = Project.builder()
            .title(req.getTitle())
            .slug(slug)
            .description(req.getDescription())
            .contentMarkdown(req.getContentMarkdown())
            .coverImageUrl(req.getCoverImageUrl())
            .projectUrl(req.getProjectUrl())
            .repoUrl(req.getRepoUrl())
            .techStack(req.getTechStack() != null ? req.getTechStack() : List.of())
            .status(req.getStatus() != null ? req.getStatus() : "in_progress")
            .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
            .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
            .startedAt(req.getStartedAt())
            .completedAt(req.getCompletedAt())
            .build();

        Project saved = projectRepository.save(project);
        log.info("Project created: id={}, slug={}", saved.getId(), saved.getSlug());
        return ApiResponse.ok(ProjectResponse.from(saved));
    }

    public ApiResponse<ProjectResponse> update(Long id, ProjectRequest req) {
        Project project = projectRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

        if (req.getTitle() != null) project.setTitle(req.getTitle());
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        if (req.getContentMarkdown() != null) project.setContentMarkdown(req.getContentMarkdown());
        if (req.getCoverImageUrl() != null) project.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getProjectUrl() != null) project.setProjectUrl(req.getProjectUrl());
        if (req.getRepoUrl() != null) project.setRepoUrl(req.getRepoUrl());
        if (req.getTechStack() != null) project.setTechStack(req.getTechStack());
        if (req.getStatus() != null) project.setStatus(req.getStatus());
        if (req.getIsFeatured() != null) project.setIsFeatured(req.getIsFeatured());
        if (req.getSortOrder() != null) project.setSortOrder(req.getSortOrder());
        if (req.getStartedAt() != null) project.setStartedAt(req.getStartedAt());
        if (req.getCompletedAt() != null) project.setCompletedAt(req.getCompletedAt());

        project = projectRepository.save(project);
        return ApiResponse.ok(ProjectResponse.from(project));
    }

    public ApiResponse<Void> delete(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
        log.info("Project soft-deleted: id={}", id);
        return ApiResponse.ok(null);
    }
}
