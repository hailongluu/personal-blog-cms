package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.config.BlogProperties;
import com.blog.cms.content.dto.NewsletterSendRequest;
import com.blog.cms.content.dto.NewsletterSendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock private NewsletterSubscriberRepository subscriberRepository;
    @Mock private NewsletterSendLogRepository sendLogRepository;
    @Mock private NewsletterEmailSender emailSender;

    private BlogProperties blogProperties;
    private NewsletterService service;

    @BeforeEach
    void setUp() {
        blogProperties = new BlogProperties();
        blogProperties.getSite().setUrl("https://task.luuhailong.com");
        service = new NewsletterService(subscriberRepository, sendLogRepository, emailSender, blogProperties);
    }

    @Test
    void send_sendsToConfirmedSubscribersOnly() {
        // Given 3 confirmed + 2 pending subscribers
        NewsletterSubscriber s1 = makeSub(1L, "a@example.com", "confirmed");
        NewsletterSubscriber s2 = makeSub(2L, "b@example.com", "confirmed");
        NewsletterSubscriber s3 = makeSub(3L, "c@example.com", "confirmed");
        when(subscriberRepository.findAllByStatus("confirmed"))
                .thenReturn(List.of(s1, s2, s3));

        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Bản tin tuần mới")
                .bodyHtml("<p>Nội dung mới</p>")
                .build();

        // When
        ApiResponse<NewsletterSendResponse> resp = service.send(req);

        // Then
        assertThat(resp.getData().getRecipientCount()).isEqualTo(3);
        assertThat(resp.getData().getSuccessCount()).isEqualTo(3);
        assertThat(resp.getData().getFailureCount()).isEqualTo(0);
        verify(emailSender, times(3)).send(any(), any(), any());
    }

    @Test
    void send_countsFailedEmails() {
        NewsletterSubscriber s1 = makeSub(1L, "a@example.com", "confirmed");
        NewsletterSubscriber s2 = makeSub(2L, "b@example.com", "confirmed");
        when(subscriberRepository.findAllByStatus("confirmed"))
                .thenReturn(List.of(s1, s2));
        // First email throws, second succeeds
        doThrow(new RuntimeException("SMTP error"))
                .doNothing()
                .when(emailSender).send(any(), any(), any());

        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Test").bodyHtml("<p>X</p>").build();

        ApiResponse<NewsletterSendResponse> resp = service.send(req);
        assertThat(resp.getData().getRecipientCount()).isEqualTo(2);
        assertThat(resp.getData().getSuccessCount()).isEqualTo(1);
        assertThat(resp.getData().getFailureCount()).isEqualTo(1);
    }

    @Test
    void send_persistsSendLog() {
        NewsletterSubscriber s1 = makeSub(1L, "a@example.com", "confirmed");
        when(subscriberRepository.findAllByStatus("confirmed"))
                .thenReturn(List.of(s1));

        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Hi").bodyHtml("<p>Y</p>").build();

        service.send(req);

        ArgumentCaptor<NewsletterSendLog> captor = ArgumentCaptor.forClass(NewsletterSendLog.class);
        verify(sendLogRepository, times(1)).save(captor.capture());
        NewsletterSendLog saved = captor.getValue();
        assertThat(saved.getSubject()).isEqualTo("Hi");
        assertThat(saved.getRecipientCount()).isEqualTo(1);
        assertThat(saved.getSuccessCount()).isEqualTo(1);
        assertThat(saved.getFailureCount()).isEqualTo(0);
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    void send_rejectsBlankSubject() {
        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("").bodyHtml("<p>X</p>").build();

        assertThatThrownBy(() -> service.send(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void send_rejectsBlankBody() {
        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Hi").bodyHtml("").build();

        assertThatThrownBy(() -> service.send(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("body");
    }

    @Test
    void send_withNoConfirmedSubscribers_returnsEmptyResult() {
        when(subscriberRepository.findAllByStatus("confirmed")).thenReturn(List.of());

        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Hi").bodyHtml("<p>Y</p>").build();

        ApiResponse<NewsletterSendResponse> resp = service.send(req);
        assertThat(resp.getData().getRecipientCount()).isZero();
        assertThat(resp.getData().getSuccessCount()).isZero();
        verify(emailSender, never()).send(any(), any(), any());
        // Still persists log so we know the admin tried to send
        verify(sendLogRepository, times(1)).save(any(NewsletterSendLog.class));
    }

    @Test
    void preview_returnsRenderedHtml() {
        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Preview Test")
                .bodyHtml("<p>Hello <strong>world</strong></p>")
                .build();
        ApiResponse<NewsletterSendResponse> resp = service.preview(req);
        assertThat(resp.getData().getPreviewHtml()).contains("Hello");
        assertThat(resp.getData().getPreviewHtml()).contains("Preview Test");
        assertThat(resp.getData().getRecipientCount()).isZero();
        verify(emailSender, never()).send(any(), any(), any());
    }

    private NewsletterSubscriber makeSub(Long id, String email, String status) {
        return NewsletterSubscriber.builder()
                .id(id)
                .email(email)
                .status(status)
                .confirmedAt(Instant.now())
                .build();
    }
}
