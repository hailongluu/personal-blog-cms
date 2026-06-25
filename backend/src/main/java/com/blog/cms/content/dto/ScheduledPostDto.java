package com.blog.cms.content.dto;

import com.blog.cms.content.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/**
 * A draft post with scheduled_at set — pending auto-publish.
 * Includes a derived `timeUntilPublish` for the UI countdown.
 */
@Data @Builder @AllArgsConstructor
public class ScheduledPostDto {
    private Long id;
    private String title;
    private String slug;
    private String status;
    private Instant scheduledAt;
    private String timeUntilPublish; // e.g. "in 2h 15m", "overdue 5m"
    private Long authorId;

    public static ScheduledPostDto from(Post p) {
        if (p == null) return null;
        Instant scheduled = p.getScheduledAt();
        String until = null;
        if (scheduled != null) {
            Duration d = Duration.between(Instant.now(), scheduled);
            if (d.isNegative()) {
                long overMin = Math.abs(d.toMinutes());
                until = "overdue " + (overMin < 60 ? overMin + "m" : (overMin / 60) + "h " + (overMin % 60) + "m");
            } else {
                long mins = d.toMinutes();
                until = "in " + (mins < 60 ? mins + "m" : (mins / 60) + "h " + (mins % 60) + "m");
            }
        }
        return ScheduledPostDto.builder()
            .id(p.getId())
            .title(p.getTitle())
            .slug(p.getSlug())
            .status(p.getStatus())
            .scheduledAt(scheduled)
            .timeUntilPublish(until)
            .authorId(p.getAuthor() != null ? p.getAuthor().getId() : null)
            .build();
    }
}
