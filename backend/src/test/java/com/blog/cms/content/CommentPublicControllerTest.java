package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CommentCreateRequest;
import com.blog.cms.content.dto.CommentResponse;
import com.blog.cms.content.dto.CommentUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommentPublicControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private CommentService commentService;

    @BeforeEach
    void setUp() {
        CommentPublicController controller = new CommentPublicController(commentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listForPost_returnsApprovedComments() throws Exception {
        CommentResponse c = CommentResponse.builder()
                .id(1L).postId(10L).authorName("A").content("hi")
                .status("approved").build();
        when(commentService.listApprovedForPost(10L)).thenReturn(ApiResponse.ok(List.of(c)));

        mockMvc.perform(get("/api/public/comments/post/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("approved"));
    }

    @Test
    void create_submitsNewComment() throws Exception {
        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L).authorName("Khách").authorEmail("k@y.com")
                .content("Bài hay quá!").build();
        CommentResponse resp = CommentResponse.builder()
                .id(99L).postId(10L).authorName("Khách").content("Bài hay quá!")
                .status("pending").build();
        when(commentService.create(any())).thenReturn(ApiResponse.ok(resp));

        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void create_returnsBadRequest_whenDisabled() throws Exception {
        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L).authorName("X").authorEmail("x@y.com")
                .content("hi").build();
        when(commentService.create(any()))
                .thenThrow(new IllegalStateException("comments are disabled for this site"));

        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("comments are disabled for this site"));
    }

    @Test
    void update_changesContent() throws Exception {
        CommentUpdateRequest req = CommentUpdateRequest.builder()
                .content("Edited content").authorEmail("a@y.com").build();
        CommentResponse resp = CommentResponse.builder()
                .id(1L).content("Edited content").status("pending").build();
        when(commentService.update(any(), any())).thenReturn(ApiResponse.ok(resp));

        mockMvc.perform(put("/api/public/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Edited content"));
    }
}
