package com.blog.cms.content.dto;

import com.blog.cms.content.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder @AllArgsConstructor
public class TagResponse {
    private Long id;
    private String name;
    private String slug;
    private Instant createdAt;

    public static TagResponse from(Tag tag) {
        return TagResponse.builder()
            .id(tag.getId())
            .name(tag.getName())
            .slug(tag.getSlug())
            .createdAt(tag.getCreatedAt())
            .build();
    }
}
