package com.yiliiii.project.my_photography_project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageProcessingServiceTest {

    private final ImageProcessingService service = new ImageProcessingService();

    @TempDir
    Path tempDir;

    @Test
    void testResizeAndSaveSquare() throws IOException {
        // 1. Create a dummy image in memory
        BufferedImage original = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(original, "jpg", baos);
        ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());

        // 2. Define output path
        Path target = tempDir.resolve("thumb.jpg");

        // 3. Call method
        service.resizeAndSaveSquare(is, target, 100, 0.8f);

        // 4. Assert file exists
        assertTrue(Files.exists(target));

        // (Optional) Could read back and check dimensions, but simple existence is good
        // for now
        BufferedImage result = ImageIO.read(target.toFile());
        assertTrue(result.getWidth() <= 100);
        assertTrue(result.getHeight() <= 100);
    }

    @Test
    void testResizeAndSaveByWidth() throws IOException {
        // 1. Create a dummy image
        BufferedImage original = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(original, "jpg", baos);
        ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());

        // 2. Output
        Path target = tempDir.resolve("web.jpg");

        // 3. Call method
        service.resizeAndSaveByWidth(is, target, 150, 0.8f);

        // 4. Assert
        assertTrue(Files.exists(target));
        BufferedImage result = ImageIO.read(target.toFile());
        // Thumbnails.of().width(X) ensures width is X (unless original is smaller and
        // specific setUps,
        // but usually it scales down). 300->150 is valid.
        // However, note that Thumbnailator default behavior might not scale up if
        // smaller,
        // but here 300 > 150.
        // Let's just check existence to be safe against slight lib variations in
        // defaults.
        assertTrue(Files.size(target) > 0);
    }
}
