package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing all scheduled-task sources from one place.
 * Mounted under /api/admin/scheduled-tasks.
 *
 * Sources:
 *   - Hermes cron jobs (system-level, exec via `hermes cron` CLI)
 *   - Content registry (collected external articles)
 *   - Scheduled draft posts (auto-publish queue)
 *   - Newsletter send log (history + resend)
 *
 * Authorization: ADMIN-only for state-changing actions (run/pause/delete/etc).
 * View endpoints (list) allow ADMIN/EDITOR to see what's queued.
 */
@RestController
@RequestMapping("/api/admin/scheduled-tasks")
@RequiredArgsConstructor
public class AdminScheduledTasksController {

    private final ScheduledTasksService scheduledTasksService;

    // ──────────────── AGGREGATE ────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<Map<String, Object>> aggregate() {
        return scheduledTasksService.aggregateView();
    }

    // ──────────────── 1. CRON JOBS ────────────────

    @GetMapping("/cron")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<List<CronJobDto>> listCronJobs() {
        return scheduledTasksService.listCronJobs();
    }

    @PostMapping("/cron/{jobId}/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> runCronJob(@PathVariable String jobId) {
        return scheduledTasksService.runCronJob(jobId);
    }

    @PostMapping("/cron/{jobId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> pauseCronJob(@PathVariable String jobId) {
        return scheduledTasksService.pauseCronJob(jobId);
    }

    @PostMapping("/cron/{jobId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> resumeCronJob(@PathVariable String jobId) {
        return scheduledTasksService.resumeCronJob(jobId);
    }

    @DeleteMapping("/cron/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteCronJob(@PathVariable String jobId) {
        return scheduledTasksService.deleteCronJob(jobId);
    }

    // ──────────────── 2. CONTENT REGISTRY ────────────────

    @GetMapping("/content-registry")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<Map<String, Object>> listContentRegistry(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String pillar,
            @RequestParam(required = false) String funnel,
            @RequestParam(required = false) String status) {
        return scheduledTasksService.listContentRegistry(page, size, source, pillar, funnel, status);
    }

    @PostMapping("/content-registry/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> rejectContentRegistry(@PathVariable Long id) {
        return scheduledTasksService.markContentRegistryRejected(id);
    }

    @DeleteMapping("/content-registry/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteContentRegistry(@PathVariable Long id) {
        return scheduledTasksService.deleteContentRegistry(id);
    }

    // ──────────────── 3. SCHEDULED POSTS ────────────────

    @GetMapping("/posts/scheduled")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<List<ScheduledPostDto>> listScheduledPosts() {
        return scheduledTasksService.listScheduledPosts();
    }

    @PostMapping("/posts/{id}/publish-now")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<PostResponse> publishPostNow(@PathVariable Long id) {
        return scheduledTasksService.publishScheduledPostNow(id);
    }

    @PostMapping("/posts/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<ScheduledPostDto> reschedulePost(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String raw = body.get("scheduledAt");
        if (raw == null || raw.isBlank()) {
            return ApiResponse.error("INVALID_INPUT: scheduledAt is required");
        }
        try {
            Instant when = Instant.parse(raw);
            return scheduledTasksService.reschedulePost(id, when);
        } catch (Exception e) {
            return ApiResponse.error("INVALID_INPUT: scheduledAt must be ISO-8601: " + e.getMessage());
        }
    }

    @PostMapping("/posts/{id}/cancel-schedule")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<ScheduledPostDto> cancelScheduledPost(@PathVariable Long id) {
        return scheduledTasksService.cancelScheduledPost(id);
    }

    // ──────────────── 4. NEWSLETTER LOG ────────────────

    @GetMapping("/newsletter")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ApiResponse<List<NewsletterLogDto>> listNewsletterLog() {
        return scheduledTasksService.listNewsletterLog();
    }

    @PostMapping("/newsletter/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resendNewsletter(@PathVariable Long id) {
        return scheduledTasksService.resendNewsletter(id);
    }

    @DeleteMapping("/newsletter/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteNewsletterLog(@PathVariable Long id) {
        return scheduledTasksService.deleteNewsletterLog(id);
    }
}
