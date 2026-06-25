package com.blog.cms.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Registry of articles collected by the content-engine cron job (e.g. GitHub Trending AI Daily).
 * Each row represents one external article that was crawled, before it's turned into a post.
 *
 * Status lifecycle:
 *   - collected: default after cron job inserts a new row
 *   - published: admin promoted it; post_id links to the created post
 *   - rejected:  admin dismissed it (e.g. low quality, duplicate)
 */
@Entity
@Table(name = "content_registry")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContentRegistry {

    public static final String STATUS_COLLECTED = "collected";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_REJECTED  = "rejected";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300, unique = true)
    private String slug;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String topic;

    @Column(length = 100)
    private String pillar;

    @Column(length = 20)
    private String funnel;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_COLLECTED;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
