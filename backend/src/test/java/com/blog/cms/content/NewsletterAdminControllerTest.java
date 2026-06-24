package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.NewsletterSendRequest;
import com.blog.cms.content.dto.NewsletterSendResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NewsletterAdminControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private NewsletterService newsletterService;

    @BeforeEach
    void setUp() {
        NewsletterAdminController controller = new NewsletterAdminController(newsletterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void send_returnsOkWithStats() throws Exception {
        NewsletterSendResponse out = NewsletterSendResponse.builder()
                .recipientCount(5)
                .successCount(5)
                .failureCount(0)
                .build();
        when(newsletterService.send(any())).thenReturn(ApiResponse.ok(out));

        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Bản tin mới")
                .bodyHtml("<p>Xin chào</p>")
                .build();

        mockMvc.perform(post("/api/admin/newsletter/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientCount").value(5))
                .andExpect(jsonPath("$.data.successCount").value(5))
                .andExpect(jsonPath("$.data.failureCount").value(0));
    }

    @Test
    void send_rejectsInvalidPayload() throws Exception {
        String badJson = "{\"subject\":\"\",\"bodyHtml\":\"\"}";
        mockMvc.perform(post("/api/admin/newsletter/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preview_returnsRenderedHtml() throws Exception {
        NewsletterSendResponse out = NewsletterSendResponse.builder()
                .recipientCount(0)
                .previewHtml("<!doctype html><html>...</html>")
                .build();
        when(newsletterService.preview(any())).thenReturn(ApiResponse.ok(out));

        NewsletterSendRequest req = NewsletterSendRequest.builder()
                .subject("Preview").bodyHtml("<p>Hello</p>").build();

        mockMvc.perform(post("/api/admin/newsletter/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewHtml").exists());
    }
}
