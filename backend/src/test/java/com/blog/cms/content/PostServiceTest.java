package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.CreatePostRequest;
import com.blog.cms.content.dto.PostResponse;
import com.blog.cms.content.dto.UpdatePostRequest;
import com.blog.cms.user.Role;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService")
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private TagRepository tagRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private PostService postService;

    private User author;
    private Post post;
    private Topic topic;
    private Tag tag;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1L).name("admin").build();
        author = User.builder().id(1L).email("admin@example.com").displayName("Admin").role(role).isActive(true).build();
        topic = Topic.builder().id(1L).name("Tech").slug("tech").build();
        tag = Tag.builder().id(1L).name("java").slug("java").build();

        post = Post.builder()
            .id(1L).title("Test Post").slug("test-post")
            .contentMarkdown("# Hello").contentHtml("<h1>Hello</h1>")
            .status("draft").visibility("public")
            .author(author).topic(topic)
            .tags(Set.of(tag))
            .readingTimeMin(1).viewCount(0L)
            .createdAt(Instant.now()).updatedAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("CREATE")
    class Create {

        @Test
        @DisplayName("should create a draft post with slug")
        void shouldCreateDraftPost() {
            when(userRepository.findByEmailWithRole("admin@example.com")).thenReturn(Optional.of(author));
            when(postRepository.existsBySlug("test-post")).thenReturn(false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
            when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
            when(postRepository.save(any(Post.class))).thenReturn(post);

            CreatePostRequest req = CreatePostRequest.builder()
                .title("Test Post").contentMarkdown("# Hello").topicId(1L).tagIds(Set.of(1L)).type("essay").build();

            ApiResponse<PostResponse> result = postService.create(req, "admin@example.com");

            assertThat(result.getError()).isNull();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getTitle()).isEqualTo("Test Post");
            assertThat(result.getData().getSlug()).isEqualTo("test-post");
            assertThat(result.getData().getStatus()).isEqualTo("draft");
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should auto-generate unique slug on conflict")
        void shouldAutoGenerateUniqueSlug() {
            when(userRepository.findByEmailWithRole("admin@example.com")).thenReturn(Optional.of(author));
            when(postRepository.existsBySlug("my-title")).thenReturn(true, false); // first conflict, second OK
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                return p; // return with the slug that was set
            });

            CreatePostRequest req = CreatePostRequest.builder()
                .title("My Title").contentMarkdown("test").type("essay").build();

            ApiResponse<PostResponse> result = postService.create(req, "admin@example.com");

            assertThat(result.getData().getSlug()).isEqualTo("my-title-1");
        }

        @Test
        @DisplayName("should set publishedAt when creating as published")
        void shouldSetPublishedAt() {
            when(userRepository.findByEmailWithRole("admin@example.com")).thenReturn(Optional.of(author));
            when(postRepository.existsBySlug(anyString())).thenReturn(false);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            CreatePostRequest req = CreatePostRequest.builder()
                .title("Published Post").status("published").contentMarkdown("test").type("essay").build();

            ApiResponse<PostResponse> result = postService.create(req, "admin@example.com");

            assertThat(result.getData().getStatus()).isEqualTo("published");
            assertThat(result.getData().getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("should fail when user not found")
        void shouldFailUserNotFound() {
            when(userRepository.findByEmailWithRole("unknown@example.com")).thenReturn(Optional.empty());

            CreatePostRequest req = CreatePostRequest.builder().title("X").type("essay").build();

            assertThatThrownBy(() -> postService.create(req, "unknown@example.com"))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("READ")
    class Read {

        @Test
        @DisplayName("should find post by slug")
        void shouldFindBySlug() {
            when(postRepository.findBySlug("test-post")).thenReturn(Optional.of(post));

            ApiResponse<PostResponse> result = postService.findBySlug("test-post");

            assertThat(result.getData().getTitle()).isEqualTo("Test Post");
            assertThat(result.getData().getAuthor().getDisplayName()).isEqualTo("Admin");
        }

        @Test
        @DisplayName("should throw on unknown slug")
        void shouldThrowOnUnknownSlug() {
            when(postRepository.findBySlug("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.findBySlug("nope"))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("UPDATE")
    class Update {

        @Test
        @DisplayName("should update title and status")
        void shouldUpdateTitleAndStatus() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdatePostRequest req = UpdatePostRequest.builder().title("Updated Title").status("published").build();

            ApiResponse<PostResponse> result = postService.update(1L, req);

            assertThat(result.getData().getTitle()).isEqualTo("Updated Title");
            assertThat(result.getData().getStatus()).isEqualTo("published");
            assertThat(result.getData().getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("should update tags")
        void shouldUpdateTags() {
            Tag newTag = Tag.builder().id(2L).name("spring").slug("spring").build();
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(tagRepository.findAllById(Set.of(2L))).thenReturn(List.of(newTag));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdatePostRequest req = UpdatePostRequest.builder().tagIds(Set.of(2L)).build();

            ApiResponse<PostResponse> result = postService.update(1L, req);

            assertThat(result.getData().getTags()).hasSize(1);
            assertThat(result.getData().getTags().get(0).getName()).isEqualTo("spring");
        }

        @Test
        @DisplayName("should fail update on deleted post")
        void shouldFailOnDeletedPost() {
            post.setDeletedAt(Instant.now());
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            UpdatePostRequest req = UpdatePostRequest.builder().title("X").build();

            assertThatThrownBy(() -> postService.update(1L, req))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("DELETE + RESTORE")
    class DeleteRestore {

        @Test
        @DisplayName("should soft-delete post")
        void shouldSoftDelete() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<Void> result = postService.softDelete(1L);

            assertThat(result.getError()).isNull();
            assertThat(post.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should restore post")
        void shouldRestore() {
            post.setDeletedAt(Instant.now());
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<Void> result = postService.restore(1L);

            assertThat(result.getError()).isNull();
            assertThat(post.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("should fail restore on non-deleted post")
        void shouldFailRestoreNonDeleted() {
            // deletedAt is null → filter exludes it
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.restore(1L))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("LIFECYCLE — publish/unpublish/archive/duplicate/preview")
    class Lifecycle {

        @Test
        @DisplayName("publish: sets status + publishedAt + firstPublishedAt + lastPublishedAt")
        void shouldPublish() {
            post.setStatus("draft");
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<PostResponse> result = postService.publish(1L);

            assertThat(result.getData().getStatus()).isEqualTo("published");
            assertThat(post.getPublishedAt()).isNotNull();
            assertThat(post.getFirstPublishedAt()).isNotNull();
            assertThat(post.getLastPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("publish: keeps firstPublishedAt on re-publish, updates lastPublishedAt")
        void shouldKeepFirstPublishedAt() {
            Instant first = Instant.parse("2026-01-01T00:00:00Z");
            post.setStatus("draft");
            post.setPublishedAt(first);
            post.setFirstPublishedAt(first);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.publish(1L);

            assertThat(post.getFirstPublishedAt()).isEqualTo(first);
            assertThat(post.getLastPublishedAt()).isAfter(first);
        }

        @Test
        @DisplayName("unpublish: PUBLISHED → DRAFT, keeps firstPublishedAt")
        void shouldUnpublish() {
            Instant first = Instant.now();
            post.setStatus("published");
            post.setFirstPublishedAt(first);
            post.setLastPublishedAt(first);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<PostResponse> result = postService.unpublish(1L);

            assertThat(result.getData().getStatus()).isEqualTo("draft");
            assertThat(post.getFirstPublishedAt()).isEqualTo(first);
        }

        @Test
        @DisplayName("archive: any non-deleted → ARCHIVED")
        void shouldArchive() {
            post.setStatus("published");
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<PostResponse> result = postService.archive(1L);

            assertThat(result.getData().getStatus()).isEqualTo("archived");
        }

        @Test
        @DisplayName("duplicate: creates a draft copy with unique slug, clears featured/canonical")
        void shouldDuplicate() {
            post.setSlug("original-slug");
            post.setStatus("published");
            post.setFeatured(true);
            post.setCanonicalUrl("https://example.com/canonical");
            post.setTopic(Topic.builder().id(1L).name("AI").slug("ai").build());

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findByEmailWithRole("admin@example.com"))
                .thenReturn(Optional.of(author));
            when(postRepository.existsBySlug("original-slug-copy")).thenReturn(false);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiResponse<PostResponse> result = postService.duplicate(1L, "admin@example.com");

            PostResponse copy = result.getData();
            assertThat(copy.getSlug()).isEqualTo("original-slug-copy");
            assertThat(copy.getStatus()).isEqualTo("draft");
            assertThat(copy.isFeatured()).isFalse();
            assertThat(copy.getCanonicalUrl()).isNull();
            assertThat(copy.getTitle()).contains("(copy)");
            assertThat(copy.getTopic()).isNotNull();
            assertThat(copy.getViewCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("preview: returns the post (works for drafts)")
        void shouldPreview() {
            post.setStatus("draft");
            when(postRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(post));

            ApiResponse<PostResponse> result = postService.preview(1L);

            assertThat(result.getData().getId()).isEqualTo(1L);
            assertThat(result.getData().getStatus()).isEqualTo("draft");
        }

        @Test
        @DisplayName("publish: fails on deleted post")
        void shouldFailPublishDeleted() {
            post.setDeletedAt(Instant.now());
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.publish(1L))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
