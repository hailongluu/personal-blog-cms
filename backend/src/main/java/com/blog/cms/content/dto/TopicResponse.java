package com.blog.cms.content.dto;

import com.blog.cms.content.Topic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data @Builder @AllArgsConstructor
public class TopicResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String color;
    private String icon;
    private Long parentId;
    private List<TopicResponse> children;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public static TopicResponse from(Topic topic) {
        return from(topic, false);
    }

    public static TopicResponse from(Topic topic, boolean includeChildren) {
        List<TopicResponse> childDtos = List.of();
        if (includeChildren) {
            try {
                var children = topic.getChildren();
                if (children != null) {
                    childDtos = children.stream().map(TopicResponse::fromChild).toList();
                }
            } catch (Exception e) {
                // lazy collection not initialized — skip
            }
        }
        return TopicResponse.builder()
            .id(topic.getId())
            .name(topic.getName())
            .slug(topic.getSlug())
            .description(topic.getDescription())
            .color(topic.getColor())
            .icon(topic.getIcon())
            .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
            .children(childDtos)
            .sortOrder(topic.getSortOrder())
            .createdAt(topic.getCreatedAt())
            .updatedAt(topic.getUpdatedAt())
            .build();
    }

    private static TopicResponse fromChild(Topic topic) {
        return TopicResponse.builder()
            .id(topic.getId())
            .name(topic.getName())
            .slug(topic.getSlug())
            .description(topic.getDescription())
            .color(topic.getColor())
            .icon(topic.getIcon())
            .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
            .sortOrder(topic.getSortOrder())
            .createdAt(topic.getCreatedAt())
            .updatedAt(topic.getUpdatedAt())
            .build();
    }
}
