package com.blog.cms.content.dto;

import com.blog.cms.content.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data @Builder @AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private String excerpt;
    private String contentMarkdown;
    private String contentHtml;
    private String coverImageUrl;
    private String ogImageUrl;
    private String status;
    private String visibility;
    private String type;
    private boolean featured;
    private AuthorDto author;
    private TopicDto topic;
    private List<TagDto> tags;
    private Integer readingTimeMin;
    private Long viewCount;
    private Instant publishedAt;
    private Instant firstPublishedAt;
    private Instant lastPublishedAt;
    private Instant scheduledAt;
    private String metaTitle;
    private String metaDescription;
    private String canonicalUrl;
    private Instant createdAt;
    private Instant updatedAt;

    @Data @Builder @AllArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String displayName;
        private String avatarUrl;
    }

    @Data @Builder @AllArgsConstructor
    public static class TopicDto {
        private Long id;
        private String name;
        private String slug;
        private String color;
    }

    @Data @Builder @AllArgsConstructor
    public static class TagDto {
        private Long id;
        private String name;
        private String slug;
    }

    public static PostResponse from(Post post) {
        return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .slug(post.getSlug())
            .subtitle(post.getSubtitle())
            .excerpt(post.getExcerpt())
            .contentMarkdown(post.getContentMarkdown())
            .contentHtml(post.getContentHtml())
            .coverImageUrl(post.getCoverImageUrl())
            .ogImageUrl(post.getOgImageUrl())
            .status(post.getStatus())
            .visibility(post.getVisibility())
            .type(post.getType() != null ? post.getType().value() : null)
            .featured(post.isFeatured())
            .author(post.getAuthor() != null ? AuthorDto.builder()
                .id(post.getAuthor().getId())
                .displayName(post.getAuthor().getDisplayName())
                .avatarUrl(post.getAuthor().getAvatarUrl())
                .build() : null)
            .topic(post.getTopic() != null ? TopicDto.builder()
                .id(post.getTopic().getId())
                .name(post.getTopic().getName())
                .slug(post.getTopic().getSlug())
                .color(post.getTopic().getColor())
                .build() : null)
            .tags(post.getTags() != null ? post.getTags().stream()
                .map(t -> TagDto.builder()
                    .id(t.getId()).name(t.getName()).slug(t.getSlug())
                    .build())
                .toList() : List.of())
            .readingTimeMin(post.getReadingTimeMin())
            .viewCount(post.getViewCount())
            .publishedAt(post.getPublishedAt())
            .firstPublishedAt(post.getFirstPublishedAt())
            .lastPublishedAt(post.getLastPublishedAt())
            .scheduledAt(post.getScheduledAt())
            .metaTitle(post.getMetaTitle())
            .metaDescription(post.getMetaDescription())
            .canonicalUrl(post.getCanonicalUrl())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}
