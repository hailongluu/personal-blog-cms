package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.NewsletterSendRequest;
import com.blog.cms.content.dto.NewsletterSendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoints for broadcasting newsletters to subscribers.
 *
 *   POST /api/admin/newsletter/send    → broadcast to all confirmed subscribers
 *   POST /api/admin/newsletter/preview → render HTML preview without sending
 *
 * Audit: every send persists a NewsletterSendLog row with subject, recipient
 * count, and success/failure tally — even zero-recipient sends.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/newsletter")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class NewsletterAdminController {

    private final NewsletterService newsletterService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NewsletterSendResponse>> send(
            @RequestBody NewsletterSendRequest req) {
        if (req == null || isBlank(req.getSubject()) || isBlank(req.getBodyHtml())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("subject and bodyHtml are required"));
        }
        log.info("Newsletter send initiated by admin: subject={}", req.getSubject());
        return ResponseEntity.ok(newsletterService.send(req));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<NewsletterSendResponse>> preview(
            @RequestBody NewsletterSendRequest req) {
        if (req == null || isBlank(req.getSubject()) || isBlank(req.getBodyHtml())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("subject and bodyHtml are required"));
        }
        return ResponseEntity.ok(newsletterService.preview(req));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
