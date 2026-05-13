package com.yiliiii.project.my_photography_project.controller;

import com.yiliiii.project.my_photography_project.dto.PhotoDetailDto;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;
import com.yiliiii.project.my_photography_project.service.AiService;
import com.yiliiii.project.my_photography_project.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PhotoApiController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private AiService aiService;

    @Autowired
    private PhotoRepository photoRepository;

    @GetMapping("/photo/{id}")
    public ResponseEntity<?> getPhotoDetails(@PathVariable Long id) {
        try {
            PhotoDetailDto photoDetail = photoService.getPhotoDetails(id);
            return ResponseEntity.ok(photoDetail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/photo/{id}/generate-ai")
    public ResponseEntity<?> generateAiDescription(@PathVariable Long id) {
        try {
            String newDescription = aiService.generateDescription(id);
            return ResponseEntity.ok(Map.of("descriptionLong", newDescription));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AI 生成失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/photo/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        try {
            photoService.deletePhotoById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.err.println("AJAX 删除照片失败 (ID: " + id + "): " + e.getMessage());
            if (e instanceof IllegalArgumentException) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/map-data")
    public ResponseEntity<List<Map<String, Object>>> getMapData() {
        List<PhotoRepository.MapPhotoProjection> allPhotos = photoRepository.findMapPhotoData();
        List<Map<String, Object>> result = new ArrayList<>();

        for (PhotoRepository.MapPhotoProjection p : allPhotos) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("title", p.getTitle());
            item.put("latitude", p.getLatitude());
            item.put("longitude", p.getLongitude());
            item.put("thumbnailUrl", p.getThumbnailUrl() != null ? p.getThumbnailUrl() : p.getImageUrl());

            if (p.getTakenAt() != null) {
                item.put("takenAt", p.getTakenAt().toLocalDate().toString());
            }
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> result = new HashMap<>();

        result.put("cameras", photoRepository.countPhotosByCamera());
        result.put("focalLengths", photoRepository.countPhotosByFocalLength());

        List<Object[]> rawHours = photoRepository.countPhotosByHour();
        Map<Integer, Long> hoursMap = new HashMap<>();
        for (Object[] row : rawHours) {
            hoursMap.put((Integer) row[0], (Long) row[1]);
        }
        List<Long> hoursData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hoursData.add(hoursMap.getOrDefault(i, 0L));
        }
        result.put("hours", hoursData);

        List<String> palettes = photoRepository.findAllColorPalettes();
        Map<String, Integer> colorStats = new HashMap<>();
        String[] baseColors = { "红色", "橙色", "黄色", "绿色", "青色", "蓝色", "紫色", "粉色", "黑白灰" };
        for (String c : baseColors) {
            colorStats.put(c, 0);
        }

        for (String palette : palettes) {
            if (palette.isEmpty()) {
                continue;
            }
            String category = classifyColor(palette);
            colorStats.put(category, colorStats.get(category) + 1);
        }

        List<Map.Entry<String, Integer>> sortedColors = new ArrayList<>(colorStats.entrySet());
        sortedColors.removeIf(e -> e.getValue() == 0);
        sortedColors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        result.put("colors", sortedColors);

        return ResponseEntity.ok(result);
    }

    private String classifyColor(String hex) {
        try {
            java.awt.Color c = java.awt.Color.decode(hex);
            float[] hsb = java.awt.Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            float hue = hsb[0] * 360;
            float sat = hsb[1] * 100;
            float bri = hsb[2] * 100;

            if (sat < 15 || bri < 15 || bri > 95) {
                return "黑白灰";
            }
            if (hue >= 345 || hue < 15) {
                return "红色";
            }
            if (hue >= 15 && hue < 45) {
                return "橙色";
            }
            if (hue >= 45 && hue < 75) {
                return "黄色";
            }
            if (hue >= 75 && hue < 155) {
                return "绿色";
            }
            if (hue >= 155 && hue < 195) {
                return "青色";
            }
            if (hue >= 195 && hue < 255) {
                return "蓝色";
            }
            if (hue >= 255 && hue < 285) {
                return "紫色";
            }
            if (hue >= 285 && hue < 345) {
                return "粉色";
            }
            return "其他";
        } catch (Exception e) {
            return "其他";
        }
    }
}
