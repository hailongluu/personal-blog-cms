package com.blog.cms.content.dto;

import com.blog.cms.content.NewsletterSendLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder @AllArgsConstructor
public class NewsletterLogDto {
    private Long id;
    private String subject;
    private int recipientCount;
    private int successCount;
    private int failureCount;
    private Long sentBy;
    private Instant sentAt;
    private String deliveryStatus; // derived: "ok" if failureCount==0 && successCount>0, else "partial"/"failed"

    public static NewsletterLogDto from(NewsletterSendLog n) {
        if (n == null) return null;
        String status;
        if (n.getSuccessCount() == 0 && n.getFailureCount() == 0) status = "empty";
        else if (n.getFailureCount() == 0) status = "ok";
        else if (n.getSuccessCount() == 0) status = "failed";
        else status = "partial";
        return NewsletterLogDto.builder()
            .id(n.getId())
            .subject(n.getSubject())
            .recipientCount(n.getRecipientCount())
            .successCount(n.getSuccessCount())
            .failureCount(n.getFailureCount())
            .sentBy(n.getSentBy())
            .sentAt(n.getSentAt())
            .deliveryStatus(status)
            .build();
    }
}
