package com.blog.cms.content;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default email sender — logs the email instead of sending.
 * In production, replace with an SMTP/SendGrid implementation by providing
 * another @Component that implements NewsletterEmailSender.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "smtpNewsletterEmailSender")
public class LoggingNewsletterEmailSender implements NewsletterEmailSender {

    @Override
    public void send(String toEmail, String subject, String htmlBody) {
        log.info("NEWSLETTER → to={} | subject={} | bodyLen={}",
                toEmail, subject, htmlBody != null ? htmlBody.length() : 0);
    }
}
