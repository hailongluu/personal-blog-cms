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
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    /** SPEC §6.4: kebab-case lowercase slug */
    private static final Pattern SLUG_REGEX =
        Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    /** SPEC §8.3.4: valid post types */
    private static final Set<String> VALID_TYPES = Set.of(
        "essay", "research_brief", "field_note", "build_log",
        "playbook", "review", "personal_log"
    );

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    // ───────────────────────────────────────────────────────────
    // Queries
    // ───────────────────────────────────────────────────────────

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
            String status, String type, Long topicId, Long authorId,
            Long tagId, Boolean featured, String search,
            String sort, int page, int size) {

        // Validate type — pass PostType enum directly (Hibernate handles enum binding)
        PostType typeFilter = null;
        if (type != null && !type.isBlank()) {
            typeFilter = PostType.parse(type);
            if (typeFilter == null) {
                throw new IllegalArgumentException("Invalid type: " + type);
            }
        }

        Sort sorting = switch (sort != null ? sort : "updated_at") {
            case "published_at" -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case "title"        -> Sort.by(Sort.Direction.ASC, "title");
            case "created_at"   -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "view_count"   -> Sort.by(Sort.Direction.DESC, "viewCount");
            default             -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
        Pageable pageable = PageRequest.of(page - 1, size, sorting);
        Page<Post> posts = postRepository.findFiltered(
            status, typeFilter, topicId, authorId, tagId, featured,
            search != null ? search.toLowerCase() : null,
            pageable);

        var data = posts.getContent().stream().map(PostResponse::from).toList();
        return ApiResponse.paged(data, page, size, posts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApiResponse<java.util.List<PostResponse>> listDeleted(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Post> posts = postRepository.findDeleted(pageable);
        var data = posts.getContent().stream().map(PostResponse::from).toList();
        return ApiResponse.paged(data, page, size, posts.getTotalElements());
    }

    // ───────────────────────────────────────────────────────────
    // Mutations
    // ───────────────────────────────────────────────────────────

    public ApiResponse<PostResponse> create(CreatePostRequest req, String userEmail) {
        User author = userRepository.findByEmailWithRole(userEmail)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String slug = req.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.slugify(req.getTitle());
        }
        slug = ensureUniqueSlug(slug);

        // Default to ESSAY when type is null/blank (e.g. legacy callers)
        PostType type = PostType.parse(req.getType());
        if (type == null) {
            type = PostType.ESSAY;
        }

        Post post = Post.builder()
            .title(req.getTitle())
            .slug(slug)
            .subtitle(req.getSubtitle())
            .excerpt(req.getExcerpt())
            .contentMarkdown(req.getContentMarkdown() != null ? req.getContentMarkdown() : "")
            .contentHtml(req.getContentHtml() != null ? req.getContentHtml() : "")
            .coverImageUrl(req.getCoverImageUrl())
            .ogImageUrl(req.getOgImageUrl())
            .status(req.getStatus() != null ? req.getStatus() : "draft")
            .visibility(req.getVisibility() != null ? req.getVisibility() : "public")
            .type(type)
            .featured(req.isFeatured())
            .author(author)
            .readingTimeMin(estimateReadingTime(req.getContentMarkdown()))
            .viewCount(0L)
            .metaTitle(req.getMetaTitle())
            .metaDescription(req.getMetaDescription())
            .canonicalUrl(req.getCanonicalUrl())
            .build();

        if (req.getTopicId() != null) {
            topicRepository.findById(req.getTopicId()).ifPresent(post::setTopic);
        }
        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            post.setTags(Set.copyOf(tagRepository.findAllById(req.getTagIds())));
        }

        // Apply publish transition if creating directly as published
        applyPublishTransition(post);

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, slug={}, type={}, status={}",
            saved.getId(), saved.getSlug(), saved.getType(), saved.getStatus());
        return ApiResponse.ok(PostResponse.from(saved));
    }

    public ApiResponse<PostResponse> update(Long id, UpdatePostRequest req) {
        Post post = postRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));

        if (req.getTitle() != null) post.setTitle(req.getTitle());
        if (req.getSubtitle() != null) post.setSubtitle(req.getSubtitle());
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
        if (req.getType() != null) {
            PostType parsed = resolveType(req.getType());
            post.setType(parsed);
        }
        if (req.getFeatured() != null) post.setFeatured(req.getFeatured());
        if (req.getMetaTitle() != null) post.setMetaTitle(req.getMetaTitle());
        if (req.getMetaDescription() != null) post.setMetaDescription(req.getMetaDescription());
        if (req.getCanonicalUrl() != null) post.setCanonicalUrl(req.getCanonicalUrl());
        if (req.getScheduledAt() != null) post.setScheduledAt(req.getScheduledAt());

        if (req.getTopicId() != null) {
            topicRepository.findById(req.getTopicId()).ifPresentOrElse(
                post::setTopic,
                () -> { /* keep existing */ }
            );
        }
        if (req.getTagIds() != null) {
            post.setTags(Set.copyOf(tagRepository.findAllById(req.getTagIds())));
        }

        applyPublishTransition(post);

        post = postRepository.save(post);
        return ApiResponse.ok(PostResponse.from(post));
    }

    // ───────────────────────────────────────────────────────────
    // Dedicated lifecycle endpoints — SPEC §8.3.10
    // ───────────────────────────────────────────────────────────

    public ApiResponse<PostResponse> publish(Long id) {
        Post post = mustExist(id);
        post.setStatus("published");
        applyPublishTransition(post);
        post = postRepository.save(post);
        log.info("Post published: id={}, slug={}", post.getId(), post.getSlug());
        return ApiResponse.ok(PostResponse.from(post));
    }

    public ApiResponse<PostResponse> unpublish(Long id) {
        Post post = mustExist(id);
        // SPEC §8.3.7: PUBLISHED → DRAFT, keep first_published_at
        post.setStatus("draft");
        post = postRepository.save(post);
        log.info("Post unpublished: id={}, slug={}", post.getId(), post.getSlug());
        return ApiResponse.ok(PostResponse.from(post));
    }

    public ApiResponse<PostResponse> archive(Long id) {
        Post post = mustExist(id);
        // SPEC §8.3.8: any non-deleted → ARCHIVED
        post.setStatus("archived");
        post = postRepository.save(post);
        log.info("Post archived: id={}, slug={}", post.getId(), post.getSlug());
        return ApiResponse.ok(PostResponse.from(post));
    }

    public ApiResponse<PostResponse> duplicate(Long id, String userEmail) {
        Post source = mustExist(id);
        User author = userRepository.findByEmailWithRole(userEmail)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String baseSlug = source.getSlug() + "-copy";
        String newSlug = ensureUniqueSlug(baseSlug);

        Post copy = Post.builder()
            .title(source.getTitle() + " (copy)")
            .slug(newSlug)
            .subtitle(source.getSubtitle())
            .excerpt(source.getExcerpt())
            .contentMarkdown(source.getContentMarkdown())
            .contentHtml(source.getContentHtml())
            .coverImageUrl(source.getCoverImageUrl())
            .ogImageUrl(source.getOgImageUrl())
            .status("draft")                    // SPEC: copies start as draft
            .visibility(source.getVisibility())
            .type(source.getType())
            .featured(false)                    // SPEC: don't carry featured flag
            .author(author)
            .topic(source.getTopic())
            .tags(source.getTags() != null ? Set.copyOf(source.getTags()) : Set.of())
            .readingTimeMin(source.getReadingTimeMin())
            .viewCount(0L)
            .metaTitle(source.getMetaTitle())
            .metaDescription(source.getMetaDescription())
            .canonicalUrl(null)                  // SPEC: clear canonical on duplicate
            .build();

        Post saved = postRepository.save(copy);
        log.info("Post duplicated: source={}, copy={}", id, saved.getId());
        return ApiResponse.ok(PostResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<PostResponse> preview(Long id) {
        // Same as findById but explicit — could allow draft preview later
        return findById(id);
    }

    // ───────────────────────────────────────────────────────────
    // Soft delete / restore
    // ───────────────────────────────────────────────────────────

    public ApiResponse<Void> softDelete(Long id) {
        Post post = mustExist(id);
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

    // ───────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────

    private Post mustExist(Long id) {
        return postRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));
    }

    private String ensureUniqueSlug(String base) {
        if (!postRepository.existsBySlug(base)) {
            return base;
        }
        int counter = 1;
        String candidate;
        do {
            candidate = base + "-" + counter++;
        } while (postRepository.existsBySlug(candidate));
        return candidate;
    }

    private PostType resolveType(String raw) {
        PostType parsed = PostType.parse(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(
                "Invalid type '" + raw + "'. Valid values: " + PostType.allValues());
        }
        return parsed;
    }

    /**
     * SPEC §8.3.6 publish rule:
     *   - set published_at = now if null
     *   - set first_published_at = now if null
     *   - set last_published_at = now (every transition)
     */
    private void applyPublishTransition(Post post) {
        if (!"published".equals(post.getStatus())) {
            return;
        }
        Instant now = Instant.now();
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }
        if (post.getFirstPublishedAt() == null) {
            post.setFirstPublishedAt(now);
        }
        post.setLastPublishedAt(now);
    }

    private int estimateReadingTime(String markdown) {
        if (markdown == null || markdown.isBlank()) return 0;
        int words = markdown.split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }
}
