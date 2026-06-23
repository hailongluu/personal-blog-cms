package com.blog.cms.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    @Query("SELECT m FROM Media m WHERE m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    Page<Media> findAllActive(Pageable pageable);
}
