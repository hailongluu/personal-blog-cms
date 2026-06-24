package com.blog.cms.content;

/**
 * Abstraction over email delivery.
 * Production implementation: SMTP/SendGrid/Mailgun via application-prod.yml config.
 * Test implementation: mock or noop.
 */
public interface NewsletterEmailSender {
    void send(String toEmail, String subject, String htmlBody);
}
