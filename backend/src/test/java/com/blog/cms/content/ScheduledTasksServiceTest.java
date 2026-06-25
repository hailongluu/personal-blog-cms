package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CronJobDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the parsing & validation helpers in ScheduledTasksService.
 * No DB / network — runs anywhere with `mvn test`.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock private ContentRegistryRepository contentRegistryRepository;
    @Mock private PostRepository postRepository;
    @Mock private NewsletterSendLogRepository newsletterSendLogRepository;
    @Mock private PostService postService;
    @Mock private NewsletterService newsletterService;

    @InjectMocks private ScheduledTasksService service;

    @Test
    void isValidJobId_acceptsHexStrings() {
        assertThat(ScheduledTasksService.isValidJobId("801687d66062")).isTrue();
        assertThat(ScheduledTasksService.isValidJobId("abcdef0123456789")).isTrue();
    }

    @Test
    void isValidJobId_rejectsInjectionAttempts() {
        assertThat(ScheduledTasksService.isValidJobId("78ccd6a8f102; rm -rf /")).isFalse();
        assertThat(ScheduledTasksService.isValidJobId("")).isFalse();
        assertThat(ScheduledTasksService.isValidJobId(null)).isFalse();
        assertThat(ScheduledTasksService.isValidJobId("../etc/passwd")).isFalse();
        assertThat(ScheduledTasksService.isValidJobId("78ccd6a8f10Z")).isFalse();
    }

    @Test
    void parseCronList_extractsSingleJob() {
        String out = """
            ┌─────────────────────────────────────────────────────────────────────────┐
            │                         Scheduled Jobs                                  │
            └─────────────────────────────────────────────────────────────────────────┘

              801687d66062 [active]
                Name:      PostgreSQL Health Monitor
                Schedule:  every 10m
                Repeat:    ∞
                Next run:  2026-06-25T10:45:01.829761+00:00
                Deliver:   origin
                Script:    pg_monitor.py
                Mode:      no-agent (script stdout delivered directly)
                Last run:  2026-06-25T10:35:01.829761+00:00  ok
            """;
        List<CronJobDto> jobs = ScheduledTasksService.parseCronList(out);
        assertThat(jobs).hasSize(1);
        CronJobDto j = jobs.get(0);
        assertThat(j.getId()).isEqualTo("801687d66062");
        assertThat(j.getState()).isEqualTo("active");
        assertThat(j.getName()).isEqualTo("PostgreSQL Health Monitor");
        assertThat(j.getSchedule()).isEqualTo("every 10m");
        assertThat(j.getNextRun()).isEqualTo("2026-06-25T10:45:01.829761+00:00");
        assertThat(j.getDeliver()).isEqualTo("origin");
        assertThat(j.getScript()).isEqualTo("pg_monitor.py");
        assertThat(j.getMode()).isEqualTo("no-agent (script stdout delivered directly)");
        assertThat(j.getLastRun()).isEqualTo("2026-06-25T10:35:01.829761+00:00");
        assertThat(j.getLastStatus()).isEqualTo("ok");
    }

    @Test
    void parseCronList_extractsMultipleJobs() {
        String out = """
              801687d66062 [active]
                Name:      PostgreSQL Health Monitor
                Schedule:  every 10m
                Last run:  2026-06-25T10:35:01.829761+00:00  ok

              78ccd6a8f102 [active]
                Name:      GitHub Trending AI Daily
                Schedule:  0 */3 * * *
                Last run:  2026-06-25T09:10:27.661255+00:00  ok
            """;
        List<CronJobDto> jobs = ScheduledTasksService.parseCronList(out);
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getId()).isEqualTo("801687d66062");
        assertThat(jobs.get(1).getId()).isEqualTo("78ccd6a8f102");
        assertThat(jobs.get(1).getSchedule()).isEqualTo("0 */3 * * *");
    }

    @Test
    void parseCronList_handlesEmptyOutput() {
        assertThat(ScheduledTasksService.parseCronList("")).isEmpty();
        assertThat(ScheduledTasksService.parseCronList(null)).isEmpty();
    }

    @Test
    void parseCronList_separatesLastStatusFromTimestamp() {
        String out = """
              78ccd6a8f102 [paused]
                Name:      Some Job
                Schedule:  0 9 * * *
                Last run:  2026-06-24T09:00:00+00:00  error
            """;
        CronJobDto j = ScheduledTasksService.parseCronList(out).get(0);
        assertThat(j.getState()).isEqualTo("paused");
        assertThat(j.getLastRun()).isEqualTo("2026-06-24T09:00:00+00:00");
        assertThat(j.getLastStatus()).isEqualTo("error");
    }

    @Test
    void aggregateView_returnsErrorWhenHermesMissing() {
        // Without hermes on PATH, listCronJobs returns error response;
        // aggregateView should still return partial view.
        // Stub scheduled posts + newsletter log so aggregateView doesn't NPE on nulls.
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(),
            PageRequest.of(0, 100), 0);
        org.mockito.Mockito.when(postRepository.findByStatusAndScheduledAtNotNull(
                org.mockito.ArgumentMatchers.eq("draft"), org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(emptyPage);
        Page<NewsletterSendLog> emptyNewsPage = new PageImpl<>(Collections.emptyList(),
            PageRequest.of(0, 100), 0);
        org.mockito.Mockito.when(newsletterSendLogRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(emptyNewsPage);
        org.mockito.Mockito.when(contentRegistryRepository.countByStatus(ContentRegistry.STATUS_COLLECTED))
            .thenReturn(0L);
        org.mockito.Mockito.when(contentRegistryRepository.countByStatus(ContentRegistry.STATUS_PUBLISHED))
            .thenReturn(0L);

        ApiResponse<Map<String, Object>> view = service.aggregateView();
        assertThat(view).isNotNull();
        // cronJobsError will be set when hermes is unavailable, data will be null
        assertThat(view.getData()).isNotNull();
        assertThat(view.getData()).containsKey("cronJobsError");
    }
}
