package com.blog.cms.content;

import com.blog.cms.content.dto.DashboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock PostRepository postRepository;
    @Mock TopicRepository topicRepository;
    @Mock TagRepository tagRepository;
    @Mock ProjectRepository projectRepository;
    @Mock MediaRepository mediaRepository;
    @Mock NewsletterSubscriberRepository newsletterSubscriberRepository;

    @InjectMocks DashboardService service;

    private Post post;

    @BeforeEach
    void setUp() {
        post = Post.builder()
            .id(1L)
            .title("Test")
            .slug("test")
            .status("published")
            .type(PostType.ESSAY)
            .featured(true)
            .updatedAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("getDashboard: aggregates counts and recent activity")
    void shouldAggregateCounts() {
        when(postRepository.countByDeletedAtIsNull()).thenReturn(10L);
        when(postRepository.countByStatusAndDeletedAtIsNull("published")).thenReturn(6L);
        when(postRepository.countByStatusAndDeletedAtIsNull("draft")).thenReturn(3L);
        when(postRepository.countByStatusAndDeletedAtIsNull("reviewing")).thenReturn(1L);
        when(postRepository.countByStatusAndDeletedAtIsNull("archived")).thenReturn(0L);
        when(topicRepository.countByDeletedAtIsNull()).thenReturn(5L);
        when(tagRepository.count()).thenReturn(12L);
        when(projectRepository.countByDeletedAtIsNull()).thenReturn(3L);
        when(mediaRepository.count()).thenReturn(45L);
        when(newsletterSubscriberRepository.countByStatus("active")).thenReturn(7L);
        when(newsletterSubscriberRepository.countByStatus("pending")).thenReturn(2L);
        when(postRepository.findRecentForDashboard(any(Pageable.class))).thenReturn(List.of(post));
        when(postRepository.findPendingDrafts(any(Pageable.class))).thenReturn(List.of());

        DashboardResponse result = service.getDashboard();

        assertThat(result.getTotalPosts()).isEqualTo(10L);
        assertThat(result.getPublishedPosts()).isEqualTo(6L);
        assertThat(result.getDraftPosts()).isEqualTo(3L);
        assertThat(result.getReviewingPosts()).isEqualTo(1L);
        assertThat(result.getTotalTopics()).isEqualTo(5L);
        assertThat(result.getTotalTags()).isEqualTo(12L);
        assertThat(result.getTotalProjects()).isEqualTo(3L);
        assertThat(result.getTotalMedia()).isEqualTo(45L);
        assertThat(result.getNewsletterSubscribers()).isEqualTo(9L);
        assertThat(result.getRecentPosts()).hasSize(1);
        assertThat(result.getRecentPosts().get(0).getTitle()).isEqualTo("Test");
        assertThat(result.getRecentPosts().get(0).isFeatured()).isTrue();
        assertThat(result.getRecentPosts().get(0).getType()).isEqualTo("essay");
        assertThat(result.getPendingDrafts()).isEmpty();
    }

    @Test
    @DisplayName("getDashboard: returns zeros when no data")
    void shouldReturnZerosWhenEmpty() {
        when(postRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(postRepository.countByStatusAndDeletedAtIsNull("published")).thenReturn(0L);
        when(postRepository.countByStatusAndDeletedAtIsNull("draft")).thenReturn(0L);
        when(postRepository.countByStatusAndDeletedAtIsNull("reviewing")).thenReturn(0L);
        when(postRepository.countByStatusAndDeletedAtIsNull("archived")).thenReturn(0L);
        when(topicRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(tagRepository.count()).thenReturn(0L);
        when(projectRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(mediaRepository.count()).thenReturn(0L);
        when(newsletterSubscriberRepository.countByStatus("active")).thenReturn(0L);
        when(newsletterSubscriberRepository.countByStatus("pending")).thenReturn(0L);
        when(postRepository.findRecentForDashboard(any(Pageable.class))).thenReturn(List.of());
        when(postRepository.findPendingDrafts(any(Pageable.class))).thenReturn(List.of());

        DashboardResponse result = service.getDashboard();

        assertThat(result.getTotalPosts()).isZero();
        assertThat(result.getRecentPosts()).isEmpty();
        assertThat(result.getNewsletterSubscribers()).isZero();
    }
}
