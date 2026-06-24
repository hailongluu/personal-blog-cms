package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CommentResponse;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommentAdminControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private CommentService commentService;

    @BeforeEach
    void setUp() {
        CommentAdminController controller = new CommentAdminController(commentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void moderationQueue_returnsPending() throws Exception {
        CommentResponse c = CommentResponse.builder()
                .id(1L).postId(10L).authorName("A").content("hi")
                .status("pending").build();
        when(commentService.listModerationQueue()).thenReturn(ApiResponse.ok(List.of(c)));

        mockMvc.perform(get("/api/admin/comments/moderation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("pending"));
    }

    @Test
    void moderate_approvesComment() throws Exception {
        CommentResponse resp = CommentResponse.builder()
                .id(1L).status("approved").build();
        when(commentService.moderate(eq(1L), eq("approved")))
                .thenReturn(ApiResponse.ok(resp));

        mockMvc.perform(post("/api/admin/comments/1/moderate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));
    }

    @Test
    void moderate_rejectsInvalidStatus() throws Exception {
        when(commentService.moderate(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("invalid status"));

        mockMvc.perform(post("/api/admin/comments/1/moderate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "weird"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid status"));
    }

    @Test
    void delete_returnsOk() throws Exception {
        when(commentService.delete(1L)).thenReturn(ApiResponse.ok(null));

        mockMvc.perform(delete("/api/admin/comments/1"))
                .andExpect(status().isOk());
    }
}
