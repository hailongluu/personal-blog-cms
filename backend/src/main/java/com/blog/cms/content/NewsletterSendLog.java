package com.blog.cms.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Audit log for every newsletter send attempt.
 * Tracks subject, recipient count, success/failure count, who sent it.
 */
@Entity
@Table(name = "newsletter_send_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsletterSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT", nullable = false)
    private String bodyHtml;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "sent_by")
    private Long sentBy;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;
}
