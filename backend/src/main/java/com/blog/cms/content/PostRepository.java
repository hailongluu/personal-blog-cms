package com.blog.cms.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.topic WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.slug = :slug AND p.deletedAt IS NULL")
    Optional<Post> findBySlug(@Param("slug") String slug);

    @Query(value = """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.topic
        WHERE p.deletedAt IS NULL
        AND (:status IS NULL OR p.status = :status)
        AND (:topicId IS NULL OR p.topic.id = :topicId)
        AND (:authorId IS NULL OR p.author.id = :authorId)
        AND (:search IS NULL OR p.title LIKE CONCAT('%', CAST(:search AS string), '%'))
        ORDER BY p.updatedAt DESC
        """,
        countQuery = """
        SELECT COUNT(p) FROM Post p
        WHERE p.deletedAt IS NULL
        AND (:status IS NULL OR p.status = :status)
        AND (:topicId IS NULL OR p.topic.id = :topicId)
        AND (:authorId IS NULL OR p.author.id = :authorId)
        AND (:search IS NULL OR p.title LIKE CONCAT('%', CAST(:search AS string), '%'))
        """)
    Page<Post> findFiltered(
        @Param("status") String status,
        @Param("topicId") Long topicId,
        @Param("authorId") Long authorId,
        @Param("search") String search,
        @Param("sort") String sort,
        Pageable pageable
    );

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NOT NULL")
    Page<Post> findDeleted(Pageable pageable);

    boolean existsBySlug(String slug);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.status = 'published' AND p.deletedAt IS NULL")
    long countPublished();
}
