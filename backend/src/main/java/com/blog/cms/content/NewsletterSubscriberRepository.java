package com.blog.cms.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber> findByEmail(String email);

    Optional<NewsletterSubscriber> findByConfirmToken(String confirmToken);

    boolean existsByEmail(String email);

    long countByStatus(String status);

    List<NewsletterSubscriber> findAllByStatus(String status);
}
