package com.blog.cms.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, String status);

    List<Comment> findByStatusOrderByCreatedAtAsc(String status);

    long countByPostIdAndStatus(Long postId, String status);
}
