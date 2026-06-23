package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.PostResponse;
import com.blog.cms.content.dto.ProjectResponse;
import com.blog.cms.content.dto.TopicResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicPostService {

    private static final Logger log = LoggerFactory.getLogger(PublicPostService.class);
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final PostRepository postRepository;
    private final TopicRepository topicRepository;
    private final ProjectRepository projectRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;

    // ──────────────────────────────────────────────
    // Posts
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<List<PostResponse>> listPublishedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Post> posts = postRepository.findPublishedPosts(pageable);
        var data = posts.getContent().stream().map(PostResponse::from).toList();
        return ApiResponse.paged(data, page, size, posts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApiResponse<PostResponse> getPublishedPostBySlug(String slug) {
        Post post = postRepository.findPublishedBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy bài viết: " + slug));
        return ApiResponse.ok(PostResponse.from(post));
    }

    @Transactional(readOnly = true)
    public String getPublishedPostMarkdown(String slug) {
        Post post = postRepository.findPublishedBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy bài viết: " + slug));
        return post.getContentMarkdown() != null ? post.getContentMarkdown() : "";
    }

    // ──────────────────────────────────────────────
    // Topics
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<List<TopicResponse>> listTopics() {
        List<Topic> topics = topicRepository.findAllActive();
        List<TopicResponse> data = topics.stream().map(TopicResponse::from).toList();
        return ApiResponse.ok(data);
    }

    @Transactional(readOnly = true)
    public ApiResponse<TopicResponse> getTopicBySlug(String slug) {
        Topic topic = topicRepository.findBySlug(slug)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy chủ đề: " + slug));
        return ApiResponse.ok(TopicResponse.from(topic, true));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PostResponse>> getPostsByTopicSlug(String slug, int page, int size) {
        // Verify topic exists
        topicRepository.findBySlug(slug)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy chủ đề: " + slug));

        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Post> posts = postRepository.findPublishedByTopicSlug(slug, pageable);
        var data = posts.getContent().stream().map(PostResponse::from).toList();
        return ApiResponse.paged(data, page, size, posts.getTotalElements());
    }

    // ──────────────────────────────────────────────
    // Projects
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<List<ProjectResponse>> listProjects(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Direction.ASC, "sortOrder")
                .and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<Project> projects = projectRepository.findAllActive(pageable);
        var data = projects.getContent().stream().map(ProjectResponse::from).toList();
        return ApiResponse.paged(data, page, size, projects.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApiResponse<ProjectResponse> getProjectBySlug(String slug) {
        Project project = projectRepository.findBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy dự án: " + slug));
        return ApiResponse.ok(ProjectResponse.from(project));
    }

    // ──────────────────────────────────────────────
    // Newsletter
    // ──────────────────────────────────────────────

    public ApiResponse<Void> subscribeNewsletter(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        email = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ: " + email);
        }
        if (newsletterSubscriberRepository.existsByEmail(email)) {
            NewsletterSubscriber existing = newsletterSubscriberRepository.findByEmail(email)
                .orElseThrow();
            if ("unsubscribed".equals(existing.getStatus())) {
                existing.setStatus("pending");
                existing.setUnsubscribedAt(null);
                newsletterSubscriberRepository.save(existing);
                log.info("Newsletter re-subscribe: {}", email);
                return ApiResponse.ok(null);
            }
            // Already subscribed — silently OK
            return ApiResponse.ok(null);
        }

        NewsletterSubscriber sub = NewsletterSubscriber.builder()
            .email(email)
            .status("pending")
            .build();
        newsletterSubscriberRepository.save(sub);
        log.info("Newsletter subscribe: {}", email);
        return ApiResponse.ok(null);
    }
}
