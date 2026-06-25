package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the four scheduled-task sources surfaced on the admin dashboard:
 *   1. Hermes cron jobs (system-level) — list/run/pause/resume/delete via `hermes cron` CLI
 *   2. Content registry (collected external articles) — list/publish/delete
 *   3. Scheduled blog posts (drafts with scheduled_at set) — publish-now/reschedule/cancel
 *   4. Newsletter send log — list/resend/delete
 *
 * Cron section uses ProcessBuilder to invoke the hermes CLI installed on the host.
 * Job IDs are validated against a strict regex to prevent command injection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTasksService {

    private static final Pattern CRON_JOB_ID = Pattern.compile("^[a-f0-9]{8,32}$");
    private static final Pattern CRON_HEADER  = Pattern.compile("^\\s+([a-f0-9]{8,32})\\s+\\[([a-z]+)\\]\\s*$");
    private static final Pattern CRON_FIELD   = Pattern.compile("^\\s+(Name|Schedule|Repeat|Next run|Deliver|Script|Mode|Last run):\\s*(.*)$");

    private final ContentRegistryRepository contentRegistryRepository;
    private final PostRepository postRepository;
    private final NewsletterSendLogRepository newsletterSendLogRepository;
    private final PostService postService;
    private final NewsletterService newsletterService;

    private String hermesBinary = "hermes";

    @PostConstruct
    void init() {
        // Allow override via system property for tests
        String override = System.getProperty("hermes.binary");
        if (override != null && !override.isBlank()) {
            this.hermesBinary = override;
        }
        log.info("ScheduledTasksService initialized — hermes binary = {}", hermesBinary);
    }

    // ──────────────────── 1. CRON JOBS ────────────────────

    public ApiResponse<List<CronJobDto>> listCronJobs() {
        try {
            String output = runHermesCommand(List.of("cron", "list"), 10);
            List<CronJobDto> jobs = parseCronList(output);
            return ApiResponse.ok(jobs);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to list cron jobs", e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return ApiResponse.error("INTERNAL_ERROR: Failed to list cron jobs: " + e.getMessage());
        }
    }

    public ApiResponse<String> runCronJob(String jobId) {
        return runHermesAction(jobId, "run");
    }

    public ApiResponse<String> pauseCronJob(String jobId) {
        return runHermesAction(jobId, "pause");
    }

    public ApiResponse<String> resumeCronJob(String jobId) {
        return runHermesAction(jobId, "resume");
    }

    @Transactional
    public ApiResponse<String> deleteCronJob(String jobId) {
        if (!isValidJobId(jobId)) {
            return ApiResponse.error("INVALID_JOB_ID: Invalid cron job id format");
        }
        try {
            String out = runHermesCommand(List.of("cron", "remove", jobId), 15);
            return ApiResponse.ok(out.isBlank() ? "deleted " + jobId : out.trim());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to delete cron job {}", jobId, e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return ApiResponse.error("INTERNAL_ERROR: Failed to delete cron job: " + e.getMessage());
        }
    }

    private ApiResponse<String> runHermesAction(String jobId, String action) {
        if (!isValidJobId(jobId)) {
            return ApiResponse.error("INVALID_JOB_ID: Invalid cron job id format");
        }
        try {
            String out = runHermesCommand(List.of("cron", action, jobId), 15);
            return ApiResponse.ok(out.isBlank() ? action + " " + jobId : out.trim());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to {} cron job {}", action, jobId, e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return ApiResponse.error("INTERNAL_ERROR: Failed to " + action + " cron job: " + e.getMessage());
        }
    }

    static boolean isValidJobId(String jobId) {
        return jobId != null && CRON_JOB_ID.matcher(jobId).matches();
    }

    private String runHermesCommand(List<String> args, int timeoutSeconds)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(hermesBinary);
        cmd.addAll(args);
        log.info("Running: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd)
            .redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + timeoutSeconds + "s");
        }
        if (process.exitValue() != 0) {
            throw new IOException("Command failed (exit " + process.exitValue() + "): " + out);
        }
        return out.toString();
    }

    /**
     * Parse the textual output of `hermes cron list`.
     * Visible for tests.
     */
    static List<CronJobDto> parseCronList(String output) {
        List<CronJobDto> jobs = new ArrayList<>();
        if (output == null || output.isBlank()) return jobs;

        Map<String, String> current = null;
        for (String line : output.split("\\r?\\n")) {
            Matcher header = CRON_HEADER.matcher(line);
            if (header.matches()) {
                if (current != null) jobs.add(toCronJobDto(current));
                current = new LinkedHashMap<>();
                current.put("id", header.group(1));
                current.put("state", header.group(2));
                continue;
            }
            if (current == null) continue;
            Matcher field = CRON_FIELD.matcher(line);
            if (field.matches()) {
                current.put(field.group(1), field.group(2).trim());
            } else if (line.trim().isEmpty()) {
                if (current != null) {
                    jobs.add(toCronJobDto(current));
                    current = null;
                }
            }
        }
        if (current != null) jobs.add(toCronJobDto(current));
        return jobs;
    }

    private static CronJobDto toCronJobDto(Map<String, String> fields) {
        String lastRun = fields.get("Last run");
        String lastStatus = null;
        if (lastRun != null && !lastRun.isBlank()) {
            int idx = lastRun.lastIndexOf("  ");
            if (idx > 0) {
                String tail = lastRun.substring(idx + 2).trim();
                if (tail.equals("ok") || tail.equals("error")) {
                    lastStatus = tail;
                    lastRun = lastRun.substring(0, idx).trim();
                }
            }
        }
        return CronJobDto.builder()
            .id(fields.get("id"))
            .state(fields.get("state"))
            .name(fields.getOrDefault("Name", ""))
            .schedule(fields.getOrDefault("Schedule", ""))
            .nextRun(fields.get("Next run"))
            .lastRun(lastRun)
            .lastStatus(lastStatus)
            .deliver(fields.get("Deliver"))
            .script(fields.get("Script"))
            .mode(fields.get("Mode"))
            .build();
    }

    // ──────────────────── 2. CONTENT REGISTRY ────────────────────

    public ApiResponse<Map<String, Object>> listContentRegistry(
            int page, int size, String source, String pillar, String funnel, String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContentRegistry> p = contentRegistryRepository.findFiltered(source, pillar, funnel, status, pageable);
        List<ContentRegistryDto> items = p.getContent().stream()
            .map(ContentRegistryDto::from)
            .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", p.getNumber());
        result.put("size", p.getSize());
        result.put("totalItems", p.getTotalElements());
        result.put("totalPages", p.getTotalPages());
        return ApiResponse.ok(result);
    }

    @Transactional
    public ApiResponse<Void> markContentRegistryRejected(Long id) {
        Optional<ContentRegistry> opt = contentRegistryRepository.findById(id);
        if (opt.isEmpty()) {
            return ApiResponse.error("NOT_FOUND: ContentRegistry " + id + " not found");
        }
        ContentRegistry c = opt.get();
        c.setStatus(ContentRegistry.STATUS_REJECTED);
        contentRegistryRepository.save(c);
        return ApiResponse.ok(null);
    }

    @Transactional
    public ApiResponse<Void> deleteContentRegistry(Long id) {
        if (!contentRegistryRepository.existsById(id)) {
            return ApiResponse.error("NOT_FOUND: ContentRegistry " + id + " not found");
        }
        contentRegistryRepository.deleteById(id);
        return ApiResponse.ok(null);
    }

    public long countContentRegistryByStatus(String status) {
        return contentRegistryRepository.countByStatus(status);
    }

    // ──────────────────── 3. SCHEDULED POSTS ────────────────────

    public ApiResponse<List<ScheduledPostDto>> listScheduledPosts() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<Post> drafts = postRepository.findByStatusAndScheduledAtNotNull("draft", pageable);
        List<ScheduledPostDto> items = drafts.getContent().stream()
            .map(ScheduledPostDto::from)
            .toList();
        return ApiResponse.ok(items);
    }

    /** Publish a scheduled draft immediately (bypassing scheduled_at). */
    public ApiResponse<PostResponse> publishScheduledPostNow(Long id) {
        Optional<Post> opt = postRepository.findById(id);
        if (opt.isEmpty()) {
            return ApiResponse.error("NOT_FOUND: Post " + id + " not found");
        }
        Post p = opt.get();
        if (!"draft".equals(p.getStatus())) {
            return ApiResponse.error("INVALID_STATE: Post " + id + " is not a draft");
        }
        return postService.publish(id);
    }

    @Transactional
    public ApiResponse<ScheduledPostDto> reschedulePost(Long id, Instant newScheduledAt) {
        if (newScheduledAt == null) {
            return ApiResponse.error("INVALID_INPUT: scheduledAt is required");
        }
        Optional<Post> opt = postRepository.findById(id);
        if (opt.isEmpty()) {
            return ApiResponse.error("NOT_FOUND: Post " + id + " not found");
        }
        Post p = opt.get();
        if (!"draft".equals(p.getStatus())) {
            return ApiResponse.error("INVALID_STATE: Post " + id + " is not a draft");
        }
        p.setScheduledAt(newScheduledAt);
        postRepository.save(p);
        return ApiResponse.ok(ScheduledPostDto.from(p));
    }

    @Transactional
    public ApiResponse<ScheduledPostDto> cancelScheduledPost(Long id) {
        Optional<Post> opt = postRepository.findById(id);
        if (opt.isEmpty()) {
            return ApiResponse.error("NOT_FOUND: Post " + id + " not found");
        }
        Post p = opt.get();
        if (!"draft".equals(p.getStatus())) {
            return ApiResponse.error("INVALID_STATE: Post " + id + " is not a draft");
        }
        p.setScheduledAt(null);
        postRepository.save(p);
        return ApiResponse.ok(ScheduledPostDto.from(p));
    }

    // ──────────────────── 4. NEWSLETTER LOG ────────────────────

    public ApiResponse<List<NewsletterLogDto>> listNewsletterLog() {
        Pageable pageable = PageRequest.of(0, 100);
        List<NewsletterLogDto> items = newsletterSendLogRepository.findAll(pageable).stream()
            .map(NewsletterLogDto::from)
            .toList();
        return ApiResponse.ok(items);
    }

    @Transactional
    public ApiResponse<Void> resendNewsletter(Long id) {
        Optional<NewsletterSendLog> opt = newsletterSendLogRepository.findById(id);
        if (opt.isEmpty()) {
            return ApiResponse.error("NOT_FOUND: Newsletter log " + id + " not found");
        }
        NewsletterSendLog log = opt.get();
        NewsletterSendRequest req = NewsletterSendRequest.builder()
            .subject(log.getSubject())
            .bodyHtml(log.getBodyHtml())
            .build();
        ApiResponse<NewsletterSendResponse> result = newsletterService.send(req);
        if (result.getData() == null) {
            return ApiResponse.error("RESEND_FAILED: " + (result.getError() != null
                ? result.getError() : "Resend failed"));
        }
        return ApiResponse.ok(null);
    }

    @Transactional
    public ApiResponse<Void> deleteNewsletterLog(Long id) {
        if (!newsletterSendLogRepository.existsById(id)) {
            return ApiResponse.error("NOT_FOUND: Newsletter log " + id + " not found");
        }
        newsletterSendLogRepository.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ──────────────────── AGGREGATE ────────────────────

    public ApiResponse<Map<String, Object>> aggregateView() {
        Map<String, Object> view = new LinkedHashMap<>();
        // Cron
        ApiResponse<List<CronJobDto>> cronResp = listCronJobs();
        view.put("cronJobs", cronResp.getData());
        view.put("cronJobsError", cronResp.getError() != null ? cronResp.getError() : null);

        // Content registry
        view.put("contentRegistryCollectedCount", countContentRegistryByStatus(ContentRegistry.STATUS_COLLECTED));
        view.put("contentRegistryPublishedCount", countContentRegistryByStatus(ContentRegistry.STATUS_PUBLISHED));

        // Scheduled posts
        ApiResponse<List<ScheduledPostDto>> postsResp = listScheduledPosts();
        view.put("scheduledPosts", postsResp.getData());

        // Newsletter log
        ApiResponse<List<NewsletterLogDto>> newsResp = listNewsletterLog();
        view.put("newsletterLog", newsResp.getData());

        return ApiResponse.ok(view);
    }

    // Helper to suppress DateTimeParseException warnings
    @SuppressWarnings("unused")
    private static Instant safeParse(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return Instant.parse(iso); } catch (DateTimeParseException e) { return null; }
    }
}
