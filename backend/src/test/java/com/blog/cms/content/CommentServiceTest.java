package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CommentCreateRequest;
import com.blog.cms.content.dto.CommentResponse;
import com.blog.cms.content.dto.CommentUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private SettingsRepository settingsRepository;

    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentRepository, postRepository, settingsRepository);
    }

    // ─────────── create ───────────

    @Test
    void create_savesPendingComment_whenCommentsEnabled() {
        Post post = Post.builder().id(10L).slug("hello").status("published").build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(settingsRepository.findById("posts.allow_comments"))
                .thenReturn(Optional.of(Setting.builder().key("posts.allow_comments")
                        .value("true").valueType("boolean").build()));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(Instant.now());
            return c;
        });

        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L)
                .authorName("Khách")
                .authorEmail("khach@example.com")
                .content("Bài viết rất hay!")
                .build();

        ApiResponse<CommentResponse> resp = service.create(req);
        assertThat(resp.getData().getId()).isEqualTo(1L);
        assertThat(resp.getData().getStatus()).isEqualTo("pending");
        assertThat(resp.getData().getContent()).isEqualTo("Bài viết rất hay!");
    }

    @Test
    void create_rejectsWhenCommentsDisabled() {
        when(settingsRepository.findById("posts.allow_comments"))
                .thenReturn(Optional.of(Setting.builder().key("posts.allow_comments")
                        .value("false").valueType("boolean").build()));

        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L).authorName("X").authorEmail("x@y.com")
                .content("...").build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void create_rejectsBlankContent() {
        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L).authorName("X").authorEmail("x@y.com")
                .content("   ").build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");
    }

    @Test
    void create_rejectsUnknownPost() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(99L).authorName("X").authorEmail("x@y.com")
                .content("hi").build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void create_rejectsCommentOnDraftPost() {
        Post post = Post.builder().id(10L).slug("draft").status("draft").build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(settingsRepository.findById("posts.allow_comments"))
                .thenReturn(Optional.of(Setting.builder().key("posts.allow_comments")
                        .value("true").valueType("boolean").build()));

        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L).authorName("X").authorEmail("x@y.com")
                .content("hi").build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not published");
    }

    @Test
    void create_storesParentForReply() {
        Post post = Post.builder().id(10L).slug("hello").status("published").build();
        Comment parent = Comment.builder().id(5L).postId(10L).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(5L)).thenReturn(Optional.of(parent));
        when(settingsRepository.findById("posts.allow_comments"))
                .thenReturn(Optional.of(Setting.builder().key("posts.allow_comments")
                        .value("true").valueType("boolean").build()));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(6L);
            return c;
        });

        CommentCreateRequest req = CommentCreateRequest.builder()
                .postId(10L).parentId(5L)
                .authorName("Replier").authorEmail("r@y.com")
                .content("Reply nè").build();

        ApiResponse<CommentResponse> resp = service.create(req);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentId()).isEqualTo(5L);
        assertThat(resp.getData().getParentId()).isEqualTo(5L);
    }

    // ─────────── list for post (public) ───────────

    @Test
    void listForPost_returnsOnlyApprovedComments() {
        Comment approved = Comment.builder().id(1L).postId(10L)
                .status("approved").authorName("A").content("ok")
                .createdAt(Instant.now()).build();
        Comment pending = Comment.builder().id(2L).postId(10L).status("pending").build();
        when(commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(10L, "approved"))
                .thenReturn(List.of(approved));

        ApiResponse<List<CommentResponse>> resp = service.listApprovedForPost(10L);
        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getStatus()).isEqualTo("approved");
    }

    // ─────────── moderation ───────────

    @Test
    void approve_setsStatusAndPersists() {
        Comment c = Comment.builder().id(1L).postId(10L).status("pending").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<CommentResponse> resp = service.moderate(1L, "approved");
        assertThat(resp.getData().getStatus()).isEqualTo("approved");
        assertThat(c.getStatus()).isEqualTo("approved");
        assertThat(c.getModeratedAt()).isNotNull();
    }

    @Test
    void reject_setsStatus() {
        Comment c = Comment.builder().id(1L).postId(10L).status("pending").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<CommentResponse> resp = service.moderate(1L, "rejected");
        assertThat(resp.getData().getStatus()).isEqualTo("rejected");
    }

    @Test
    void moderate_rejectsInvalidStatus() {
        assertThatThrownBy(() -> service.moderate(1L, "weird"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listModerationQueue_returnsPending() {
        Comment c1 = Comment.builder().id(1L).status("pending").postId(10L)
                .authorName("A").content("x").createdAt(Instant.now()).build();
        when(commentRepository.findByStatusOrderByCreatedAtAsc("pending"))
                .thenReturn(List.of(c1));

        ApiResponse<List<CommentResponse>> resp = service.listModerationQueue();
        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getData().get(0).getStatus()).isEqualTo("pending");
    }

    // ─────────── delete ───────────

    @Test
    void delete_marksAsDeleted_softDelete() {
        Comment c = Comment.builder().id(1L).postId(10L).status("approved").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(1L);
        assertThat(c.getStatus()).isEqualTo("deleted");
        assertThat(c.getDeletedAt()).isNotNull();
    }

    @Test
    void update_changesContentWithin5Minutes() {
        Comment c = Comment.builder().id(1L).postId(10L).status("pending")
                .authorEmail("a@y.com").authorName("A")
                .createdAt(Instant.now().minusSeconds(60))
                .content("Old content")
                .build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentUpdateRequest req = CommentUpdateRequest.builder()
                .content("New content").authorEmail("a@y.com").build();
        ApiResponse<CommentResponse> resp = service.update(1L, req);
        assertThat(resp.getData().getContent()).isEqualTo("New content");
    }

    @Test
    void update_rejectsAfter5Minutes() {
        Comment c = Comment.builder().id(1L).postId(10L)
                .authorEmail("a@y.com")
                .createdAt(Instant.now().minusSeconds(600))
                .content("Old").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

        CommentUpdateRequest req = CommentUpdateRequest.builder()
                .content("New").authorEmail("a@y.com").build();

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("edit window");
    }
}
