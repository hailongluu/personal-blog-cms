package com.blog.cms.content;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageVariantService {

    private static final Logger log = LoggerFactory.getLogger(ImageVariantService.class);
    private static final int THUMBNAIL_MAX_WIDTH = 300;
    private static final float WEBP_QUALITY = 0.8f;

    @Value("${blog.upload.dir:/data/uploads}")
    private String uploadDir;

    @PostConstruct
    void init() {
        ImageIO.scanForPlugins(); // registers webp-imageio
    }

    /**
     * Generate a thumbnail (max 300px wide, aspect ratio preserved).
     * Returns the relative storage path, or null if generation fails.
     */
    public String generateThumbnail(Path sourceFile, String yearMonth, String baseName) {
        try {
            BufferedImage original = ImageIO.read(sourceFile.toFile());
            if (original == null) {
                log.warn("Cannot read image for thumbnail: {}", sourceFile);
                return null;
            }

            int thumbWidth = Math.min(original.getWidth(), THUMBNAIL_MAX_WIDTH);
            int thumbHeight = (int) ((double) thumbWidth / original.getWidth() * original.getHeight());

            BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, thumbWidth, thumbHeight, null);
            g.dispose();

            String thumbFileName = "thumb_" + baseName + ".jpg";
            Path targetDir = Path.of(uploadDir, yearMonth);
            Files.createDirectories(targetDir);
            Path thumbFile = targetDir.resolve(thumbFileName);
            ImageIO.write(thumbnail, "JPEG", thumbFile.toFile());

            log.info("Thumbnail generated: {} ({}x{})", thumbFile, thumbWidth, thumbHeight);
            return yearMonth + "/" + thumbFileName;
        } catch (IOException e) {
            log.warn("Failed to generate thumbnail for {}: {}", sourceFile, e.getMessage());
            return null;
        }
    }

    /**
     * Generate a WebP version of the image.
     * Returns the relative storage path, or null if generation fails.
     */
    public String generateWebP(Path sourceFile, String yearMonth, String baseName) {
        try {
            BufferedImage image = ImageIO.read(sourceFile.toFile());
            if (image == null) {
                log.warn("Cannot read image for WebP: {}", sourceFile);
                return null;
            }

            // Convert to RGB if needed (WebP encoder requires RGB)
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgb.createGraphics().drawImage(image, 0, 0, null);

            String webpFileName = baseName + ".webp";
            Path targetDir = Path.of(uploadDir, yearMonth);
            Files.createDirectories(targetDir);
            Path webpFile = targetDir.resolve(webpFileName);

            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            try (OutputStream os = Files.newOutputStream(webpFile);
                 ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType("Lossy");
                param.setCompressionQuality(WEBP_QUALITY);
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }

            long size = Files.size(webpFile);
            log.info("WebP generated: {} ({} bytes)", webpFile, size);
            return yearMonth + "/" + webpFileName;
        } catch (IOException e) {
            log.warn("Failed to generate WebP for {}: {}", sourceFile, e.getMessage());
            return null;
        }
    }

    /**
     * Generate both thumbnail and WebP variants.
     * Failures are logged but do not block — individual variants may be null.
     */
    public VariantResult generateVariants(Path sourceFile, String yearMonth, String baseName) {
        String thumbnailPath = generateThumbnail(sourceFile, yearMonth, baseName);
        String webpPath = generateWebP(sourceFile, yearMonth, baseName);
        return new VariantResult(thumbnailPath, webpPath);
    }

    public record VariantResult(String thumbnailPath, String webpPath) {}
}
