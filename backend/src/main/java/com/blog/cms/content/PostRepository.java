package com.blog.cms.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ───────────────────────────────────────────────────────────
    // Admin filters — supports status/topic/author/type/tag/featured/search
    // Sort comes from Pageable (sort column mapped to entity field via whitelist).
    // ───────────────────────────────────────────────────────────

    @Query(value = """
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        LEFT JOIN p.tags t
        WHERE p.deletedAt IS NULL
        AND (:status IS NULL OR p.status = :status)
        AND (:type   IS NULL OR p.type = :type)
        AND (:topicId   IS NULL OR p.topic.id  = :topicId)
        AND (:authorId  IS NULL OR p.author.id = :authorId)
        AND (:tagId     IS NULL OR t.id = :tagId)
        AND (:featured  IS NULL OR p.featured = :featured)
        AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT p) FROM Post p
        LEFT JOIN p.tags t
        WHERE p.deletedAt IS NULL
        AND (:status IS NULL OR p.status = :status)
        AND (:type   IS NULL OR p.type = :type)
        AND (:topicId   IS NULL OR p.topic.id  = :topicId)
        AND (:authorId  IS NULL OR p.author.id = :authorId)
        AND (:tagId     IS NULL OR t.id = :tagId)
        AND (:featured  IS NULL OR p.featured = :featured)
        AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """)
    Page<Post> findFiltered(
        @Param("status")   String status,
        @Param("type")     PostType type,
        @Param("topicId")  Long topicId,
        @Param("authorId") Long authorId,
        @Param("tagId")    Long tagId,
        @Param("featured") Boolean featured,
        @Param("search")   String search,
        Pageable pageable
    );

    // ───────────────────────────────────────────────────────────
    // Single post lookups
    // ───────────────────────────────────────────────────────────

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.topic WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.slug = :slug AND p.deletedAt IS NULL")
    Optional<Post> findBySlug(@Param("slug") String slug);

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NOT NULL")
    Page<Post> findDeleted(Pageable pageable);

    boolean existsBySlug(String slug);

    // ───────────────────────────────────────────────────────────
    // Full-text search using tsvector + ts_rank
    // ───────────────────────────────────────────────────────────

    @Query(value = """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        LEFT JOIN p.tags t
        WHERE p.deletedAt IS NULL
        AND p.searchVector IS NOT NULL
        AND function('plainto_tsquery', 'simple', :query) IS NOT NULL
        AND function('ts_rank', p.searchVector, function('plainto_tsquery', 'simple', :query)) > 0
        AND (:status IS NULL OR p.status = :status)
        AND (:type   IS NULL OR p.type = :type)
        ORDER BY function('ts_rank', p.searchVector, function('plainto_tsquery', 'simple', :query)) DESC,
                 p.publishedAt DESC
        """,
        countQuery = """
        SELECT COUNT(p) FROM Post p
        LEFT JOIN p.tags t
        WHERE p.deletedAt IS NULL
        AND function('plainto_tsquery', 'simple', :query) IS NOT NULL
        AND function('ts_rank', p.searchVector, function('plainto_tsquery', 'simple', :query)) > 0
        AND (:status IS NULL OR p.status = :status)
        AND (:type   IS NULL OR p.type = :type)
        """)
    Page<Post> searchFullText(
        @Param("query") String query,
        @Param("status") String status,
        @Param("type") PostType type,
        Pageable pageable
    );

    // ───────────────────────────────────────────────────────────
    // Dashboard counts — SPEC §8.2
    // ───────────────────────────────────────────────────────────

    long countByDeletedAtIsNull();

    long countByStatusAndDeletedAtIsNull(String status);

    long countByTypeAndStatusAndDeletedAtIsNull(PostType type, String status);

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
    List<Post> findRecentForDashboard(Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE p.deletedAt IS NULL
        AND p.status IN ('draft', 'reviewing')
        ORDER BY p.updatedAt DESC
        """)
    List<Post> findPendingDrafts(Pageable pageable);

    // ───────────────────────────────────────────────────────────
    // Public API queries — only PUBLISHED + visibility=public + not deleted
    // ───────────────────────────────────────────────────────────

    @Query(value = """
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        LEFT JOIN p.tags t
        WHERE p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        AND p.publishedAt IS NOT NULL
        AND p.publishedAt <= :now
        AND (:type  IS NULL OR p.type = :type)
        AND (:tagId IS NULL OR t.id = :tagId)
        ORDER BY p.publishedAt DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT p) FROM Post p
        LEFT JOIN p.tags t
        WHERE p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        AND p.publishedAt IS NOT NULL
        AND p.publishedAt <= :now
        AND (:type  IS NULL OR p.type = :type)
        AND (:tagId IS NULL OR t.id = :tagId)
        """)
    Page<Post> findPublishedPosts(
        @Param("now") Instant now,
        @Param("type") PostType type,
        @Param("tagId") Long tagId,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        WHERE p.slug = :slug
        AND p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        """)
    Optional<Post> findPublishedBySlug(@Param("slug") String slug);

    @Query(value = """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        WHERE p.topic.slug = :topicSlug
        AND p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        AND p.publishedAt IS NOT NULL
        AND p.publishedAt <= :now
        ORDER BY p.publishedAt DESC
        """,
        countQuery = """
        SELECT COUNT(p) FROM Post p
        WHERE p.topic.slug = :topicSlug
        AND p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        AND p.publishedAt IS NOT NULL
        AND p.publishedAt <= :now
        """)
    Page<Post> findPublishedByTopicSlug(
        @Param("topicSlug") String topicSlug,
        @Param("now") Instant now,
        Pageable pageable
    );

    @Query(value = """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        WHERE p.featured = TRUE
        AND p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        AND p.publishedAt IS NOT NULL
        AND p.publishedAt <= :now
        ORDER BY COALESCE(p.lastPublishedAt, p.publishedAt) DESC
        """,
        countQuery = """
        SELECT COUNT(p) FROM Post p
        WHERE p.featured = TRUE
        AND p.status = 'published'
        AND p.visibility = 'public'
        AND p.deletedAt IS NULL
        AND p.publishedAt IS NOT NULL
        AND p.publishedAt <= :now
        """)
    Page<Post> findFeaturedPosts(@Param("now") Instant now, Pageable pageable);
}
