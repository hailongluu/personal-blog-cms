package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.ProjectRequest;
import com.blog.cms.content.dto.ProjectResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService")
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @InjectMocks private ProjectService projectService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder()
            .id(1L).title("My Project").slug("my-project")
            .description("A project").status("in_progress")
            .isFeatured(false).sortOrder(0)
            .techStack(List.of("java", "spring"))
            .build();
    }

    @Test
    @DisplayName("should list projects with pagination")
    void shouldListProjects() {
        Page<Project> page = new PageImpl<>(List.of(project));
        when(projectRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        ApiResponse<List<ProjectResponse>> result = projectService.list(1, 20);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getMeta().getTotalItems()).isEqualTo(1);
        assertThat(result.getData().get(0).getTitle()).isEqualTo("My Project");
    }

    @Test
    @DisplayName("should create project")
    void shouldCreateProject() {
        when(projectRepository.existsBySlug("my-project")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectRequest req = ProjectRequest.builder()
            .title("My Project").description("A project").techStack(List.of("java")).build();

        ApiResponse<ProjectResponse> result = projectService.create(req);

        assertThat(result.getData().getTitle()).isEqualTo("My Project");
        assertThat(result.getData().getSlug()).isEqualTo("my-project");
        assertThat(result.getData().getTechStack()).contains("java");
    }

    @Test
    @DisplayName("should soft-delete project")
    void shouldSoftDelete() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Void> result = projectService.delete(1L);

        assertThat(result.getError()).isNull();
        assertThat(project.getDeletedAt()).isNotNull();
    }
}
