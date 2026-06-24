package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.MediaResponse;
import com.blog.cms.security.BlogUserDetails;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService")
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private UserRepository userRepository;
    @Mock private ImageVariantService imageVariantService;
    @InjectMocks private MediaService mediaService;

    private User uploader;
    private Media media;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "uploadDir", "/tmp/test-uploads");

        Role role = Role.builder().id(1L).name("admin").build();
        uploader = User.builder().id(1L).email("admin@example.com").displayName("Admin").role(role).build();

        media = Media.builder()
            .id(1L).filename("abc123.jpg").originalName("photo.jpg")
            .mimeType("image/jpeg").sizeBytes(1024L)
            .storagePath("2026/06/abc123.jpg").publicUrl("/uploads/2026/06/abc123.jpg")
            .uploadedBy(uploader)
            .build();
    }

    @Test
    @DisplayName("should list media with pagination")
    void shouldListMedia() {
        Page<Media> page = new PageImpl<>(List.of(media));
        when(mediaRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        ApiResponse<List<MediaResponse>> result = mediaService.list(1, 10);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getOriginalName()).isEqualTo("photo.jpg");
    }

    @Test
    @DisplayName("should reject empty file")
    void shouldRejectEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> mediaService.upload(file, "admin@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("should reject unsupported MIME type")
    void shouldRejectBadType() {
        MultipartFile file = new MockMultipartFile("file", "bad.exe", "application/x-msdownload", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> mediaService.upload(file, "admin@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported");
    }

    @Test
    @DisplayName("should soft-delete media")
    void shouldSoftDelete() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Void> result = mediaService.delete(1L);

        assertThat(result.getError()).isNull();
        assertThat(media.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("should generate image variants on upload")
    void shouldGenerateVariantsOnUpload() {
        byte[] imageBytes = new byte[100]; // minimal image bytes
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", imageBytes);

        when(userRepository.findByEmailWithRole("admin@example.com")).thenReturn(Optional.of(uploader));
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageVariantService.VariantResult variants = new ImageVariantService.VariantResult(
            "2026/06/thumb_abc.jpg", "2026/06/abc.webp"
        );
        when(imageVariantService.generateVariants(any(), eq("2026/06"), anyString())).thenReturn(variants);

        ApiResponse<MediaResponse> result = mediaService.upload(file, "admin@example.com");

        assertThat(result.getData().getThumbnailUrl()).isEqualTo("/uploads/2026/06/thumb_abc.jpg");
        assertThat(result.getData().getWebpUrl()).isEqualTo("/uploads/2026/06/abc.webp");
        verify(imageVariantService).generateVariants(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("should skip variant generation for non-image files")
    void shouldSkipVariantsForNonImage() {
        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);

        when(userRepository.findByEmailWithRole("admin@example.com")).thenReturn(Optional.of(uploader));
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<MediaResponse> result = mediaService.upload(file, "admin@example.com");

        assertThat(result.getData().getThumbnailUrl()).isNull();
        assertThat(result.getData().getWebpUrl()).isNull();
        verify(imageVariantService, never()).generateVariants(any(), anyString(), anyString());
    }
}
