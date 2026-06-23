package com.blog.cms.content;

import com.blog.cms.content.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates dashboard counters and recent activity — SPEC §8.2.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PostRepository postRepository;
    private final TopicRepository topicRepository;
    private final TagRepository tagRepository;
    private final ProjectRepository projectRepository;
    private final MediaRepository mediaRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;

    public DashboardResponse getDashboard() {
        List<Post> recent = postRepository.findRecentForDashboard(PageRequest.of(0, 5));
        List<Post> drafts = postRepository.findPendingDrafts(PageRequest.of(0, 5));

        long newsletterCount = newsletterSubscriberRepository.countByStatus("active")
            + newsletterSubscriberRepository.countByStatus("pending");

        return DashboardResponse.builder()
            .totalPosts(postRepository.countByDeletedAtIsNull())
            .publishedPosts(postRepository.countByStatusAndDeletedAtIsNull("published"))
            .draftPosts(postRepository.countByStatusAndDeletedAtIsNull("draft"))
            .reviewingPosts(postRepository.countByStatusAndDeletedAtIsNull("reviewing"))
            .archivedPosts(postRepository.countByStatusAndDeletedAtIsNull("archived"))
            .totalTopics(topicRepository.countByDeletedAtIsNull())
            .totalTags(tagRepository.count())
            .totalProjects(projectRepository.countByDeletedAtIsNull())
            .totalMedia(mediaRepository.count())
            .newsletterSubscribers(newsletterCount)
            .recentPosts(recent.stream().map(this::toRecent).toList())
            .pendingDrafts(drafts.stream().map(this::toRecent).toList())
            .build();
    }

    private DashboardResponse.RecentPostDto toRecent(Post p) {
        return DashboardResponse.RecentPostDto.builder()
            .id(p.getId())
            .title(p.getTitle())
            .slug(p.getSlug())
            .status(p.getStatus())
            .type(p.getType() != null ? p.getType().value() : null)
            .featured(p.isFeatured())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}
