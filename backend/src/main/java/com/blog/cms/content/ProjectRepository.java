package com.blog.cms.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE p.slug = :slug AND p.deletedAt IS NULL")
    Optional<Project> findBySlug(@Param("slug") String slug);

    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL ORDER BY p.sortOrder ASC, p.createdAt DESC")
    Page<Project> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.isFeatured = true AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    Page<Project> findFeatured(Pageable pageable);

    boolean existsBySlug(String slug);
}
