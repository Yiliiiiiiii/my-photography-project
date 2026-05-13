package com.yiliiii.project.my_photography_project.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageProcessingService {

    /**
     * 保存原图（直接拷贝流）
     */
    public void saveOriginal(InputStream inputStream, Path targetPath) throws IOException {
        Files.copy(inputStream, targetPath);
    }

    /**
     * 调整大小并保存 (指定正方形尺寸，通常用于缩略图)
     */
    public void resizeAndSaveSquare(InputStream inputStream, Path targetPath, int size, float quality)
            throws IOException {
        Thumbnails.of(inputStream)
                .size(size, size)
                .outputQuality(quality)
                .outputFormat("jpeg")
                .toFile(targetPath.toFile());
    }

    /**
     * 调整大小并保存 (指定宽度，高度自适应，通常用于Web展示大图)
     */
    public void resizeAndSaveByWidth(InputStream inputStream, Path targetPath, int width, float quality)
            throws IOException {
        Thumbnails.of(inputStream)
                .width(width)
                .outputQuality(quality)
                .outputFormat("jpeg")
                .toFile(targetPath.toFile());
    }
}
