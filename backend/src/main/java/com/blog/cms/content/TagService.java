package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.TagRequest;
import com.blog.cms.content.dto.TagResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public ApiResponse<List<TagResponse>> list() {
        List<Tag> tags = tagRepository.findAll();
        return ApiResponse.ok(tags.stream().map(TagResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public ApiResponse<TagResponse> findById(Long id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Tag not found: " + id));
        return ApiResponse.ok(TagResponse.from(tag));
    }

    public ApiResponse<TagResponse> create(TagRequest req) {
        if (tagRepository.existsByName(req.getName())) {
            throw new IllegalArgumentException("Tag name already exists: " + req.getName());
        }
        String slug = req.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.slugify(req.getName());
        }
        int counter = 1;
        String base = slug;
        while (tagRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }

        Tag tag = Tag.builder()
            .name(req.getName())
            .slug(slug)
            .build();
        Tag saved = tagRepository.save(tag);
        log.info("Tag created: id={}, slug={}", saved.getId(), saved.getSlug());
        return ApiResponse.ok(TagResponse.from(saved));
    }

    public ApiResponse<Void> delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new EntityNotFoundException("Tag not found: " + id);
        }
        tagRepository.deleteById(id);
        log.info("Tag deleted: id={}", id);
        return ApiResponse.ok(null);
    }
}
