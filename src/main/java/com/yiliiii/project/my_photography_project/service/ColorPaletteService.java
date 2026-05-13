package com.yiliiii.project.my_photography_project.service;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
//import java.util.stream.Collectors;
import java.awt.Color;

/**
 * 智能色板提取服务 (高密度版)
 * 使用 9x9x9 RGB 网格生成 729 种标准色，降低颜色重合度。
 */
@Service
public class ColorPaletteService {

    private static final int K = 5; // 提取5种主色
    private static final int MAX_ITERATIONS = 20;
    private static final int RESIZE_WIDTH = 100;

    // 标准颜色列表
    private static final List<Color> STANDARD_PALETTE = new ArrayList<>();

    static {
        // 【修改点】: 算法生成高密度色盘
        // 步长 32: 0, 32, 64, 96, 128, 160, 192, 224, 255
        // 总共 9 * 9 * 9 = 729 种颜色
        // 这比之前的 140 种多了 5 倍，区分度大大提高
        int step = 32;
        for (int r = 0; r <= 256; r += step) {
            for (int g = 0; g <= 256; g += step) {
                for (int b = 0; b <= 256; b += step) {
                    // 修正超过 255 的情况 (确保包含纯白 255)
                    int finalR = Math.min(r, 255);
                    int finalG = Math.min(g, 255);
                    int finalB = Math.min(b, 255);

                    STANDARD_PALETTE.add(new Color(finalR, finalG, finalB));
                }
            }
        }
    }

    public String extractDominantColors(byte[] imageBytes) {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null)
                return null;

            BufferedImage resizedImage = resizeImage(originalImage, RESIZE_WIDTH);
            List<int[]> pixels = getPixels(resizedImage);

            // 1. K-Means 算出原始主色
            List<int[]> centroids = runKMeans(pixels, K);

            // 2. 吸附到最近的标准色
            Set<String> snappedColors = new LinkedHashSet<>();
            for (int[] rgb : centroids) {
                Color exactColor = new Color(rgb[0], rgb[1], rgb[2]);
                Color nearest = findNearestStandardColor(exactColor);
                snappedColors.add(colorToHex(nearest));
            }

            return String.join(",", snappedColors);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 寻找最近的标准色
    private Color findNearestStandardColor(Color target) {
        Color nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Color standard : STANDARD_PALETTE) {
            double dist = colorDistance(target, standard);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = standard;
            }
        }
        return nearest;
    }

    // 颜色距离 (加权欧几里得，更符合人眼感知)
    // 人眼对绿色的敏感度最高，对蓝色最低
    private double colorDistance(Color c1, Color c2) {
        double rMean = (c1.getRed() + c2.getRed()) / 2.0;
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();

        return Math.sqrt((2 + rMean / 256) * r * r + 4 * g * g + (2 + (255 - rMean) / 256) * b * b);
    }

    // --- 辅助方法 (不变) ---

    private BufferedImage resizeImage(BufferedImage original, int width) {
        int height = (int) ((double) original.getHeight() / original.getWidth() * width);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private List<int[]> getPixels(BufferedImage image) {
        List<int[]> pixels = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = image.getRGB(i, j);
                Color c = new Color(rgb);
                pixels.add(new int[] { c.getRed(), c.getGreen(), c.getBlue() });
            }
        }
        return pixels;
    }

    private List<int[]> runKMeans(List<int[]> pixels, int k) {
        if (pixels.isEmpty())
            return new ArrayList<>();
        Random random = new Random();
        List<int[]> centroids = new ArrayList<>();

        // 防止像素点少于 K 个
        int actualK = Math.min(k, pixels.size());

        for (int i = 0; i < actualK; i++) {
            centroids.add(pixels.get(random.nextInt(pixels.size())));
        }

        int[] assignments = new int[pixels.size()];

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            boolean changed = false;
            for (int i = 0; i < pixels.size(); i++) {
                int[] pixel = pixels.get(i);
                int bestCentroid = -1;
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < centroids.size(); j++) {
                    // 简单的欧几里得距离用于 K-Means 内部聚类
                    double dist = Math.pow(pixel[0] - centroids.get(j)[0], 2) +
                            Math.pow(pixel[1] - centroids.get(j)[1], 2) +
                            Math.pow(pixel[2] - centroids.get(j)[2], 2);
                    if (dist < minDist) {
                        minDist = dist;
                        bestCentroid = j;
                    }
                }
                if (assignments[i] != bestCentroid) {
                    assignments[i] = bestCentroid;
                    changed = true;
                }
            }
            if (!changed)
                break;

            int[][] sums = new int[actualK][3];
            int[] counts = new int[actualK];
            for (int i = 0; i < pixels.size(); i++) {
                int cluster = assignments[i];
                if (cluster != -1) {
                    sums[cluster][0] += pixels.get(i)[0];
                    sums[cluster][1] += pixels.get(i)[1];
                    sums[cluster][2] += pixels.get(i)[2];
                    counts[cluster]++;
                }
            }
            for (int j = 0; j < actualK; j++) {
                if (counts[j] > 0) {
                    centroids.set(j,
                            new int[] { sums[j][0] / counts[j], sums[j][1] / counts[j], sums[j][2] / counts[j] });
                }
            }
        }
        return centroids;
    }

    private String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}