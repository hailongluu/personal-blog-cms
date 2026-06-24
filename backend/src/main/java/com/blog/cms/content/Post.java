package com.blog.cms.content;

import com.blog.cms.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 280)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String contentMarkdown = "";

    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String contentHtml = "";

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "og_image_url", columnDefinition = "TEXT")
    private String ogImageUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "draft"; // draft|reviewing|published|archived

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String visibility = "public"; // public|unlisted|private

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_posts_author"))
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", foreignKey = @ForeignKey(name = "fk_posts_topic"))
    private Topic topic;

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "reading_time_min", nullable = false)
    @Builder.Default
    private Integer readingTimeMin = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "first_published_at")
    private Instant firstPublishedAt;

    @Column(name = "last_published_at")
    private Instant lastPublishedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    // --- Type & featured — SPEC §8.3.3 + §8.3.4 ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PostType type = PostType.ESSAY;

    @Column(length = 1000)
    private String subtitle;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    // --- SEO ---
    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "canonical_url", columnDefinition = "TEXT")
    private String canonicalUrl;

    // --- Full-text search (generated tsvector column) ---
    @Column(name = "search_vector", insertable = false, updatable = false, columnDefinition = "TEXT")
    @org.hibernate.annotations.Generated(org.hibernate.annotations.GenerationTime.INSERT)
    private String searchVector;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
