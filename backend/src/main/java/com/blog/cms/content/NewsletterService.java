package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.NewsletterSendRequest;
import com.blog.cms.content.dto.NewsletterSendResponse;
import com.blog.cms.config.BlogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Newsletter broadcast service.
 *
 * Sends HTML email to all confirmed subscribers. Persists an audit log entry
 * for every send (even zero-recipient sends) so admins have a history.
 *
 * Email delivery is abstracted via {@link NewsletterEmailSender} so we can swap
 * implementations (log-only in dev, SMTP/SendGrid in prod) without touching
 * the service logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final NewsletterSendLogRepository sendLogRepository;
    private final NewsletterEmailSender emailSender;
    private final BlogProperties blogProperties;

    @Transactional
    public ApiResponse<NewsletterSendResponse> send(NewsletterSendRequest req) {
        validate(req);
        List<NewsletterSubscriber> recipients = subscriberRepository.findAllByStatus("confirmed");
        return dispatch(req, recipients, /*persistLog=*/ true, /*sendEmails=*/ true);
    }

    @Transactional(readOnly = true)
    public ApiResponse<NewsletterSendResponse> preview(NewsletterSendRequest req) {
        validate(req);
        // No recipients, no email — just render the HTML preview
        String rendered = renderHtml(req, /*unsubscribeLink=*/ "https://example.com/unsubscribe");
        NewsletterSendResponse out = NewsletterSendResponse.builder()
                .recipientCount(0)
                .successCount(0)
                .failureCount(0)
                .previewHtml(rendered)
                .build();
        return ApiResponse.ok(out);
    }

    // ─────────── helpers ───────────

    private ApiResponse<NewsletterSendResponse> dispatch(
            NewsletterSendRequest req,
            List<NewsletterSubscriber> recipients,
            boolean persistLog,
            boolean sendEmails) {

        int success = 0;
        int failure = 0;

        if (sendEmails) {
            for (NewsletterSubscriber sub : recipients) {
                String unsub = blogProperties.getSite().getUrl()
                        + "/newsletter/unsubscribe?email="
                        + urlEncode(sub.getEmail());
                String html = renderHtml(req, unsub);
                try {
                    emailSender.send(sub.getEmail(), req.getSubject(), html);
                    success++;
                } catch (Exception ex) {
                    failure++;
                    log.warn("Newsletter send failed for {} — {}", sub.getEmail(), ex.getMessage());
                }
            }
        }

        NewsletterSendResponse out = NewsletterSendResponse.builder()
                .recipientCount(recipients.size())
                .successCount(success)
                .failureCount(failure)
                .previewHtml(null)
                .build();

        if (persistLog) {
            NewsletterSendLog logEntry = NewsletterSendLog.builder()
                    .subject(req.getSubject())
                    .bodyHtml(req.getBodyHtml())
                    .recipientCount(recipients.size())
                    .successCount(success)
                    .failureCount(failure)
                    .sentAt(Instant.now())
                    .build();
            sendLogRepository.save(logEntry);
        }

        return ApiResponse.ok(out);
    }

    private void validate(NewsletterSendRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request is null");
        }
        if (req.getSubject() == null || req.getSubject().isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (req.getBodyHtml() == null || req.getBodyHtml().isBlank()) {
            throw new IllegalArgumentException("bodyHtml must not be blank");
        }
    }

    private String renderHtml(NewsletterSendRequest req, String unsubscribeLink) {
        return "<!doctype html><html><body style=\"font-family:system-ui,sans-serif\">"
                + "<h2>" + escape(req.getSubject()) + "</h2>"
                + "<div>" + req.getBodyHtml() + "</div>"
                + "<hr style=\"margin-top:32px\"><small style=\"color:#888\">"
                + "<a href=\"" + escape(unsubscribeLink) + "\">Hủy đăng ký</a>"
                + "</small></body></html>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
