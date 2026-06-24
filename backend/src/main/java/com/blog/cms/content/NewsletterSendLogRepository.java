package com.blog.cms.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsletterSendLogRepository extends JpaRepository<NewsletterSendLog, Long> {
}
