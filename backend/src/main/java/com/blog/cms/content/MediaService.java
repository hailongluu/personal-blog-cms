package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.MediaResponse;
import com.blog.cms.user.User;
import com.blog.cms.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif",
        "image/svg+xml", "application/pdf"
    );
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final ImageVariantService imageVariantService;

    @Value("${blog.upload.dir:/data/uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public ApiResponse<List<MediaResponse>> list(int page, int size) {
        Page<Media> media = mediaRepository.findAllActive(PageRequest.of(page - 1, size));
        var data = media.getContent().stream().map(MediaResponse::from).toList();
        return ApiResponse.paged(data, page, size, media.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApiResponse<MediaResponse> findById(Long id) {
        Media media = mediaRepository.findById(id)
            .filter(m -> m.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Media not found: " + id));
        return ApiResponse.ok(MediaResponse.from(media));
    }

    public ApiResponse<MediaResponse> upload(MultipartFile file, String userEmail) {
        // Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large. Max: 20MB");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType + ". Allowed: " + ALLOWED_MIME_TYPES);
        }

        User uploader = userRepository.findByEmailWithRole(userEmail)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Generate storage path: yyyy/mm/uuid.ext
        var now = java.time.LocalDate.now();
        String yearMonth = String.format("%04d/%02d", now.getYear(), now.getMonthValue());
        String ext = getExtension(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String filename = uuid + ext;
        String relativePath = yearMonth + "/" + filename;
        String thumbnailPath = null, webpPath = null;

        try {
            Path targetDir = Path.of(uploadDir, yearMonth);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            file.transferTo(targetFile.toFile());
            log.info("File saved: {}", targetFile);

            // Generate image variants (thumbnail + WebP)
            String baseName = uuid; // filename without extension
            if (isImage(mimeType)) {
                ImageVariantService.VariantResult variants = imageVariantService.generateVariants(targetFile, yearMonth, baseName);
                thumbnailPath = variants.thumbnailPath();
                webpPath = variants.webpPath();
            }
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new RuntimeException("Failed to store file", e);
        }

        // Save metadata
        Media media = Media.builder()
            .filename(filename)
            .originalName(file.getOriginalFilename())
            .mimeType(mimeType)
            .sizeBytes(file.getSize())
            .storagePath(relativePath)
            .publicUrl("/uploads/" + relativePath)
            .thumbnailPath(thumbnailPath)
            .thumbnailUrl(thumbnailPath != null ? "/uploads/" + thumbnailPath : null)
            .webpPath(webpPath)
            .webpUrl(webpPath != null ? "/uploads/" + webpPath : null)
            .uploadedBy(uploader)
            .build();

        Media saved = mediaRepository.save(media);
        log.info("Media uploaded: id={}, file={}, size={}", saved.getId(), saved.getOriginalName(), saved.getSizeBytes());
        return ApiResponse.ok(MediaResponse.from(saved));
    }

    public ApiResponse<Void> delete(Long id) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media not found: " + id));
        media.setDeletedAt(Instant.now());
        mediaRepository.save(media);

        // Attempt to delete file (best-effort)
        try {
            Path filePath = Path.of(uploadDir, media.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete file for media id={}: {}", id, e.getMessage());
        }

        log.info("Media soft-deleted: id={}", id);
        return ApiResponse.ok(null);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}
