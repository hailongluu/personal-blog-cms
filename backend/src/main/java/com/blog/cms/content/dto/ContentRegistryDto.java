package com.blog.cms.content.dto;

import com.blog.cms.content.ContentRegistry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder @AllArgsConstructor
public class ContentRegistryDto {
    private Long id;
    private String slug;
    private String source;
    private String sourceUrl;
    private String topic;
    private String pillar;
    private String funnel;
    private String status;
    private Long postId;
    private Instant publishedAt;
    private Instant createdAt;

    public static ContentRegistryDto from(ContentRegistry c) {
        if (c == null) return null;
        return ContentRegistryDto.builder()
            .id(c.getId())
            .slug(c.getSlug())
            .source(c.getSource())
            .sourceUrl(c.getSourceUrl())
            .topic(c.getTopic())
            .pillar(c.getPillar())
            .funnel(c.getFunnel())
            .status(c.getStatus())
            .postId(c.getPostId())
            .publishedAt(c.getPublishedAt())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
