package com.blog.cms.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRegistryRepository extends JpaRepository<ContentRegistry, Long> {

    Optional<ContentRegistry> findBySlug(String slug);

    Page<ContentRegistry> findByStatus(String status, Pageable pageable);

    @Query("""
        SELECT c FROM ContentRegistry c
        WHERE (:source IS NULL OR c.source = :source)
          AND (:pillar IS NULL OR c.pillar = :pillar)
          AND (:funnel IS NULL OR c.funnel = :funnel)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.publishedAt DESC
        """)
    Page<ContentRegistry> findFiltered(
        @Param("source") String source,
        @Param("pillar") String pillar,
        @Param("funnel") String funnel,
        @Param("status") String status,
        Pageable pageable
    );

    @Query("SELECT DISTINCT c.source FROM ContentRegistry c WHERE c.source IS NOT NULL ORDER BY c.source")
    List<String> findDistinctSources();

    long countByStatus(String status);
}
