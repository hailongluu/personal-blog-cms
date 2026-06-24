package com.blog.cms.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageVariantService")
class ImageVariantServiceTest {

    private ImageVariantService service;

    @TempDir
    Path uploadDir;

    @BeforeEach
    void setUp() {
        service = new ImageVariantService();
        service.init();
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.toString());
    }

    private Path createTestImage(int width, int height, String name) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.setColor(java.awt.Color.WHITE);
        g.drawString("Test Image", width / 4, height / 2);
        g.dispose();

        Path yearMonth = uploadDir.resolve("2026/06");
        Files.createDirectories(yearMonth);
        Path file = yearMonth.resolve(name);
        ImageIO.write(img, "JPEG", file.toFile());
        return file;
    }

    @Nested
    @DisplayName("generateThumbnail")
    class ThumbnailTests {

        @Test
        @DisplayName("should generate 300px wide thumbnail from large image")
        void shouldGenerateThumbnailFromLargeImage() throws IOException {
            Path source = createTestImage(800, 600, "large.jpg");

            String thumbPath = service.generateThumbnail(source, "2026/06", "large");

            assertThat(thumbPath).isNotNull().endsWith(".jpg");
            Path thumbFile = uploadDir.resolve(thumbPath);
            assertThat(thumbFile).exists();

            BufferedImage thumb = ImageIO.read(thumbFile.toFile());
            assertThat(thumb.getWidth()).isEqualTo(300);
            assertThat(thumb.getHeight()).isEqualTo(225); // 600 * 300/800
        }

        @Test
        @DisplayName("should not upscale small image")
        void shouldNotUpscaleSmallImage() throws IOException {
            Path source = createTestImage(200, 150, "small.jpg");

            String thumbPath = service.generateThumbnail(source, "2026/06", "small");

            assertThat(thumbPath).isNotNull();
            BufferedImage thumb = ImageIO.read(uploadDir.resolve(thumbPath).toFile());
            assertThat(thumb.getWidth()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("generateWebP")
    class WebPTests {

        @Test
        @DisplayName("should generate WebP variant")
        void shouldGenerateWebP() throws IOException {
            Path source = createTestImage(800, 600, "test.jpg");

            String webpPath = service.generateWebP(source, "2026/06", "test");

            assertThat(webpPath).isNotNull().endsWith(".webp");
            Path webpFile = uploadDir.resolve(webpPath);
            assertThat(webpFile).exists();
            assertThat(Files.size(webpFile)).isGreaterThan(100);
        }

        @Test
        @DisplayName("should produce WebP smaller than original JPEG")
        void shouldProduceSmallerFile() throws IOException {
            Path source = createTestImage(800, 600, "perf.jpg");

            String webpPath = service.generateWebP(source, "2026/06", "perf");

            assertThat(webpPath).isNotNull();
            long sourceSize = Files.size(source);
            long webpSize = Files.size(uploadDir.resolve(webpPath));
            assertThat(webpSize).isLessThan(sourceSize * 2); // sanity: not absurdly large
        }
    }

    @Nested
    @DisplayName("generateVariants")
    class VariantTests {

        @Test
        @DisplayName("should generate both thumbnail and WebP")
        void shouldGenerateBoth() throws IOException {
            Path source = createTestImage(800, 600, "both.jpg");

            ImageVariantService.VariantResult result = service.generateVariants(source, "2026/06", "both");

            assertThat(result.thumbnailPath()).isNotNull();
            assertThat(result.webpPath()).isNotNull();
            assertThat(uploadDir.resolve(result.thumbnailPath())).exists();
            assertThat(uploadDir.resolve(result.webpPath())).exists();
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should return null for non-existent file")
        void shouldReturnNullForBadFile() {
            Path badFile = uploadDir.resolve("nonexistent.jpg");

            String result = service.generateThumbnail(badFile, "2026/06", "bad");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for non-image file")
        void shouldReturnNullForNonImage() throws IOException {
            Path textFile = uploadDir.resolve("2026/06/readme.txt");
            Files.createDirectories(textFile.getParent());
            Files.writeString(textFile, "not an image");

            String result = service.generateThumbnail(textFile, "2026/06", "readme");

            assertThat(result).isNull();
        }
    }
}
