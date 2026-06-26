package com.blog.cms.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "newsletter_subscribers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending"; // pending|confirmed|unsubscribed|bounced

    @Column(name = "confirm_token", length = 100)
    private String confirmToken;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "unsubscribed_at")
    private Instant unsubscribedAt;

    // ip_address is INET type in Postgres — read-only from entity.
    // Newsletter signup from public API doesn't provide IP; column left for future use.
    @Column(name = "ip_address", length = 45, insertable = false, updatable = false)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
