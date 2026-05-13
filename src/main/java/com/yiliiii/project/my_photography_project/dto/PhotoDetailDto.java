package com.yiliiii.project.my_photography_project.dto;
import com.yiliiii.project.my_photography_project.entity.Album;
import com.yiliiii.project.my_photography_project.entity.Comment;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.Tag;
 // 1. 你的包名

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 【DTO】: 照片详情的数据传输对象
 */
public class PhotoDetailDto {
    
    // Photo 字段
    private Long id;
    private String title;
    private String descriptionLong; 
    private String imageUrl;
    
    // EXIF 字段
    private String cameraModel;
    private String aperture;
    private String iso;
    private String shutterSpeed;
    private String cameraMake;
    private String takenAt; // (格式化后的字符串)

    // Uploader 字段
    private String uploaderUsername;
    private String uploaderAvatarUrl;

    // 关系字段
    private Set<String> tags; 
    private int likeCount;
    private List<CommentDto> comments; 

    // 原始图片 URL
    private String originalImageUrl;

    // 【新增】色板字段
    private String colorPalette;

    // 【新增】这张照片所属的相册 ID 列表
    private Set<Long> containingAlbumIds = new HashSet<>();

    // 日期格式化器
    private static final DateTimeFormatter TAKEN_AT_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm");

    // 构造函数 (只应该有一个)
    public PhotoDetailDto(Photo photo) {
        // 映射 Photo 字段
        this.id = photo.getId();
        this.title = photo.getTitle();
        this.descriptionLong = photo.getDescriptionLong();
        
        // 优先使用网页优化图
        String webUrl = photo.getWebImageUrl();
        if (webUrl != null && !webUrl.isEmpty()) {
            this.imageUrl = webUrl; 
        } else {
            this.imageUrl = photo.getImageUrl(); 
        }

        // 映射 "原始图" (用于 "查看原图" 链接)
        this.originalImageUrl = photo.getImageUrl();

        // 【新增】映射色板
        this.colorPalette = photo.getColorPalette();

        // 映射 EXIF
        this.cameraModel = photo.getCameraModel();
        this.aperture = photo.getAperture();
        this.iso = photo.getIso();
        this.shutterSpeed = photo.getShutterSpeed();
        this.cameraMake = photo.getCameraMake();
        
        if (photo.getTakenAt() != null) {
            LocalDateTime ldt = photo.getTakenAt();
            this.takenAt = "拍摄于 " + ldt.format(TAKEN_AT_FORMATTER);
        } else {
            this.takenAt = "拍摄时间未知";
        }

        // 映射关系
        this.likeCount = photo.getLikeCount();
        
        this.tags = photo.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        
        // 将 Set<Comment> 转换为 List<CommentDto>
        this.comments = photo.getComments().stream()
                .map(CommentDto::new) 
                .collect(Collectors.toList());
                
        // 映射 Uploader 信息
        if (photo.getUploader() != null) {
            this.uploaderUsername = photo.getUploader().getUsername();
            String avatar = photo.getUploader().getAvatarUrl();
            this.uploaderAvatarUrl = (avatar != null && !avatar.isEmpty()) 
                                     ? avatar 
                                     : "/images/default_avatar.png";
        } else {
            this.uploaderUsername = "未知用户";
            this.uploaderAvatarUrl = "/images/default_avatar.png";
        }

        // 【新增】映射相册 IDs
        if (photo.getAlbums() != null) {
            this.containingAlbumIds = photo.getAlbums().stream()
                .map(Album::getId)
                .collect(Collectors.toSet());
        }
    }
    

    // --- Getters ---
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescriptionLong() { return descriptionLong; }
    public String getImageUrl() { return imageUrl; }
    public String getCameraModel() { return cameraModel; }
    public String getCameraMake() { return cameraMake; }
    public String getAperture() { return aperture; }
    public String getIso() { return iso; }
    public String getShutterSpeed() { return shutterSpeed; }
    public Set<String> getTags() { return tags; }
    public int getLikeCount() { return likeCount; }
    public List<CommentDto> getComments() { return comments; }
    public String getUploaderUsername() { return uploaderUsername; }
    public String getUploaderAvatarUrl() { return uploaderAvatarUrl; }
    public String getTakenAt() { return takenAt; }
    public String getOriginalImageUrl() { return originalImageUrl; }
    
    // 【新增】Getter
    public String getColorPalette() { return colorPalette; }

    // 【新增 Getter】
    public Set<Long> getContainingAlbumIds() { return containingAlbumIds; }
    
}