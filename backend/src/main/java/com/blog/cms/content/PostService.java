package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.*;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
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

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ApiResponse<PostResponse> findById(Long id) {
        Post post = postRepository.findByIdWithRelations(id)
            .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));
        return ApiResponse.ok(PostResponse.from(post));
    }

    @Transactional(readOnly = true)
    public ApiResponse<PostResponse> findBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException("Post not found: " + slug));
        return ApiResponse.ok(PostResponse.from(post));
    }

    @Transactional(readOnly = true)
    public ApiResponse<java.util.List<PostResponse>> list(
            String status, Long topicId, Long authorId, String search,
            String sort, int page, int size) {

        Sort sorting = switch (sort != null ? sort : "updated_at") {
            case "published_at" -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
        Pageable pageable = PageRequest.of(page - 1, size, sorting);
        Page<Post> posts = postRepository.findFiltered(status, topicId, authorId,
            search != null ? search.toLowerCase() : null,
            sort != null ? sort : "updated_at", pageable);

        var data = posts.getContent().stream().map(PostResponse::from).toList();
        return ApiResponse.paged(data, page, size, posts.getTotalElements());
    }

    public ApiResponse<PostResponse> create(CreatePostRequest req, String userEmail) {
        User author = userRepository.findByEmailWithRole(userEmail)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String slug = req.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.slugify(req.getTitle());
        }
        // ensure unique slug
        int counter = 1;
        String base = slug;
        while (postRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }

        Post post = Post.builder()
            .title(req.getTitle())
            .slug(slug)
            .excerpt(req.getExcerpt())
            .contentMarkdown(req.getContentMarkdown() != null ? req.getContentMarkdown() : "")
            .contentHtml(req.getContentHtml() != null ? req.getContentHtml() : "")
            .coverImageUrl(req.getCoverImageUrl())
            .ogImageUrl(req.getOgImageUrl())
            .status(req.getStatus() != null ? req.getStatus() : "draft")
            .visibility(req.getVisibility() != null ? req.getVisibility() : "public")
            .author(author)
            .readingTimeMin(estimateReadingTime(req.getContentMarkdown()))
            .viewCount(0L)
            .metaTitle(req.getMetaTitle())
            .metaDescription(req.getMetaDescription())
            .canonicalUrl(req.getCanonicalUrl())
            .build();

        // Set topic
        if (req.getTopicId() != null) {
            topicRepository.findById(req.getTopicId()).ifPresent(post::setTopic);
        }
        // Set tags
        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            post.setTags(Set.copyOf(tagRepository.findAllById(req.getTagIds())));
        }

        // If publishing now, set publishedAt
        if ("published".equals(post.getStatus())) {
            post.setPublishedAt(Instant.now());
        }

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, slug={}, status={}", saved.getId(), saved.getSlug(), saved.getStatus());
        return ApiResponse.ok(PostResponse.from(saved));
    }

    public ApiResponse<PostResponse> update(Long id, UpdatePostRequest req) {
        Post post = postRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));

        boolean wasNotPublished = !"published".equals(post.getStatus());
        boolean nowPublishing = "published".equals(req.getStatus());

        if (req.getTitle() != null) post.setTitle(req.getTitle());
        if (req.getExcerpt() != null) post.setExcerpt(req.getExcerpt());
        if (req.getContentMarkdown() != null) {
            post.setContentMarkdown(req.getContentMarkdown());
            post.setReadingTimeMin(estimateReadingTime(req.getContentMarkdown()));
        }
        if (req.getContentHtml() != null) post.setContentHtml(req.getContentHtml());
        if (req.getCoverImageUrl() != null) post.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getOgImageUrl() != null) post.setOgImageUrl(req.getOgImageUrl());
        if (req.getStatus() != null) post.setStatus(req.getStatus());
        if (req.getVisibility() != null) post.setVisibility(req.getVisibility());
        if (req.getMetaTitle() != null) post.setMetaTitle(req.getMetaTitle());
        if (req.getMetaDescription() != null) post.setMetaDescription(req.getMetaDescription());
        if (req.getCanonicalUrl() != null) post.setCanonicalUrl(req.getCanonicalUrl());

        // Topic
        if (req.getTopicId() != null) {
            topicRepository.findById(req.getTopicId()).ifPresentOrElse(
                post::setTopic,
                () -> { /* keep existing if not found */ }
            );
        }
        // Tags
        if (req.getTagIds() != null) {
            post.setTags(Set.copyOf(tagRepository.findAllById(req.getTagIds())));
        }
        // Scheduled publish
        if (req.getScheduledAt() != null) {
            post.setScheduledAt(req.getScheduledAt());
        }

        // Publish transition
        if (wasNotPublished && nowPublishing && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        post = postRepository.save(post);
        return ApiResponse.ok(PostResponse.from(post));
    }

    public ApiResponse<Void> softDelete(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));
        post.setDeletedAt(Instant.now());
        postRepository.save(post);
        log.info("Post soft-deleted: id={}", id);
        return ApiResponse.ok(null);
    }

    public ApiResponse<Void> restore(Long id) {
        Post post = postRepository.findById(id)
            .filter(p -> p.getDeletedAt() != null)
            .orElseThrow(() -> new EntityNotFoundException("Post not found or not deleted: " + id));
        post.setDeletedAt(null);
        postRepository.save(post);
        log.info("Post restored: id={}", id);
        return ApiResponse.ok(null);
    }

    @Transactional(readOnly = true)
    public ApiResponse<java.util.List<PostResponse>> listDeleted(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Post> posts = postRepository.findDeleted(pageable);
        var data = posts.getContent().stream().map(PostResponse::from).toList();
        return ApiResponse.paged(data, page, size, posts.getTotalElements());
    }

    // --- helpers ---

    private int estimateReadingTime(String markdown) {
        if (markdown == null || markdown.isBlank()) return 0;
        int words = markdown.split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }
}
