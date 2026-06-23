package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.TopicRequest;
import com.blog.cms.content.dto.TopicResponse;
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
@DisplayName("TopicService")
class TopicServiceTest {

    @Mock private TopicRepository topicRepository;
    @InjectMocks private TopicService topicService;

    private Topic techTopic;

    @BeforeEach
    void setUp() {
        techTopic = Topic.builder().id(1L).name("Tech").slug("tech").sortOrder(0).build();
    }

    @Nested
    @DisplayName("LIST")
    class ListTopics {
        @Test
        @DisplayName("should return all active topics")
        void shouldReturnAllActive() {
            when(topicRepository.findAllActive()).thenReturn(List.of(techTopic));

            ApiResponse<List<TopicResponse>> result = topicService.list();

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getName()).isEqualTo("Tech");
        }
    }

    @Nested
    @DisplayName("CREATE")
    class Create {
        @Test
        @DisplayName("should create topic with slug")
        void shouldCreateTopic() {
            when(topicRepository.existsBySlug("tech")).thenReturn(false);
            when(topicRepository.save(any(Topic.class))).thenReturn(techTopic);

            TopicRequest req = TopicRequest.builder().name("Tech").slug("tech").build();

            ApiResponse<TopicResponse> result = topicService.create(req);

            assertThat(result.getData().getName()).isEqualTo("Tech");
            assertThat(result.getData().getSlug()).isEqualTo("tech");
        }

        @Test
        @DisplayName("should auto-gen slug from name")
        void shouldAutoGenSlug() {
            when(topicRepository.existsBySlug("new-topic")).thenReturn(false);
            when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
                Topic t = inv.getArgument(0);
                t.setId(2L);
                return t;
            });

            TopicRequest req = TopicRequest.builder().name("New Topic").build();

            ApiResponse<TopicResponse> result = topicService.create(req);

            assertThat(result.getData().getSlug()).isEqualTo("new-topic");
        }
    }

    @Nested
    @DisplayName("UPDATE")
    class Update {
        @Test
        @DisplayName("should update topic name")
        void shouldUpdateName() {
            when(topicRepository.findById(1L)).thenReturn(Optional.of(techTopic));
            when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

            TopicRequest req = TopicRequest.builder().name("Technology").build();

            ApiResponse<TopicResponse> result = topicService.update(1L, req);

            assertThat(result.getData().getName()).isEqualTo("Technology");
        }
    }

    @Nested
    @DisplayName("DELETE")
    class Delete {
        @Test
        @DisplayName("should soft-delete topic")
        void shouldSoftDelete() {
            when(topicRepository.findById(1L)).thenReturn(Optional.of(techTopic));
            when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<Void> result = topicService.delete(1L);

            assertThat(result.getError()).isNull();
            assertThat(techTopic.getDeletedAt()).isNotNull();
        }
    }
}
