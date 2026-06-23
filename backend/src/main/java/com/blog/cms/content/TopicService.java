package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.TopicRequest;
import com.blog.cms.content.dto.TopicResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);
    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public ApiResponse<List<TopicResponse>> list() {
        List<Topic> topics = topicRepository.findAllActive();
        List<TopicResponse> data = topics.stream().map(TopicResponse::from).toList();
        return ApiResponse.ok(data);
    }

    @Transactional(readOnly = true)
    public ApiResponse<TopicResponse> findById(Long id) {
        Topic topic = topicRepository.findByIdWithChildren(id)
            .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        return ApiResponse.ok(TopicResponse.from(topic, true));
    }

    public ApiResponse<TopicResponse> create(TopicRequest req) {
        // Check name uniqueness
        if (topicRepository.findBySlug(SlugUtils.slugify(req.getName())).isPresent()) {
            throw new IllegalArgumentException("Topic name already exists: " + req.getName());
        }
        String slug = req.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.slugify(req.getName());
        }
        int counter = 1;
        String base = slug;
        while (topicRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }

        Topic topic = Topic.builder()
            .name(req.getName())
            .slug(slug)
            .description(req.getDescription())
            .color(req.getColor())
            .icon(req.getIcon())
            .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
            .build();

        if (req.getParentId() != null) {
            topicRepository.findById(req.getParentId()).ifPresent(topic::setParent);
        }

        Topic saved = topicRepository.save(topic);
        log.info("Topic created: id={}, slug={}", saved.getId(), saved.getSlug());
        return ApiResponse.ok(TopicResponse.from(saved));
    }

    public ApiResponse<TopicResponse> update(Long id, TopicRequest req) {
        Topic topic = topicRepository.findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));

        if (req.getName() != null) topic.setName(req.getName());
        if (req.getDescription() != null) topic.setDescription(req.getDescription());
        if (req.getColor() != null) topic.setColor(req.getColor());
        if (req.getIcon() != null) topic.setIcon(req.getIcon());
        if (req.getSortOrder() != null) topic.setSortOrder(req.getSortOrder());
        if (req.getParentId() != null) {
            topicRepository.findById(req.getParentId()).ifPresentOrElse(
                topic::setParent,
                () -> topic.setParent(null)
            );
        }

        final var saved = topicRepository.save(topic);
        return ApiResponse.ok(TopicResponse.from(saved));
    }

    public ApiResponse<Void> delete(Long id) {
        Topic topic = topicRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        topic.setDeletedAt(Instant.now());
        topicRepository.save(topic);
        log.info("Topic soft-deleted: id={}", id);
        return ApiResponse.ok(null);
    }
}
