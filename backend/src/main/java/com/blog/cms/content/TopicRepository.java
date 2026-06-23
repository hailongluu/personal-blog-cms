package com.blog.cms.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.children WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<Topic> findByIdWithChildren(@Param("id") Long id);

    Optional<Topic> findBySlug(String slug);

    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.parent WHERE t.deletedAt IS NULL ORDER BY t.sortOrder ASC")
    List<Topic> findAllActive();

    List<Topic> findByParentId(Long parentId);

    boolean existsBySlug(String slug);
}
