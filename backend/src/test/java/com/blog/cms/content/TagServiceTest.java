package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.TagRequest;
import com.blog.cms.content.dto.TagResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService")
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @InjectMocks private TagService tagService;

    private Tag javaTag;

    @BeforeEach
    void setUp() {
        javaTag = Tag.builder().id(1L).name("java").slug("java").build();
    }

    @Test
    @DisplayName("should list all tags")
    void shouldListAll() {
        when(tagRepository.findAll()).thenReturn(List.of(javaTag));

        ApiResponse<List<TagResponse>> result = tagService.list();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("java");
    }

    @Test
    @DisplayName("should create tag")
    void shouldCreateTag() {
        when(tagRepository.existsBySlug("rust")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(Tag.builder().id(2L).name("rust").slug("rust").build());

        TagRequest req = TagRequest.builder().name("rust").build();

        ApiResponse<TagResponse> result = tagService.create(req);

        assertThat(result.getData().getName()).isEqualTo("rust");
        assertThat(result.getData().getSlug()).isEqualTo("rust");
    }

    @Test
    @DisplayName("should delete tag")
    void shouldDeleteTag() {
        when(tagRepository.existsById(1L)).thenReturn(true);
        doNothing().when(tagRepository).deleteById(1L);

        ApiResponse<Void> result = tagService.delete(1L);

        assertThat(result.getError()).isNull();
        verify(tagRepository).deleteById(1L);
    }
}
