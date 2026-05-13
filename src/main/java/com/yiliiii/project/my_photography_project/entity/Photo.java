package com.yiliiii.project.my_photography_project.entity; // 1. 你的包名

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "photo")
public class Photo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String descriptionShort;

    @Column(columnDefinition = "TEXT")
    private String descriptionLong;

    private String imageUrl;
    private String thumbnailUrl;
    private String webImageUrl;

    // EXIF 数据
    private String cameraModel;
    private String aperture;
    private String iso;
    private String shutterSpeed;
    private String cameraMake;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    // 【新增】地理位置
    private Double latitude;
    private Double longitude;

    // 【新增】智能色板 (存为逗号分隔的字符串: "#FF0000,#00FF00...")
    private String colorPalette;

    private String focalLength; // 【新增】焦段 (例如 "35 mm")

    // 【新增】记录生成缩略图/Web图时使用的压缩质量 (例如 0.9)
    // 用于后续维护时判断是否需要重新压缩
    private Float compressionQuality;

    // --- 关系 ---
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "photo_tags", joinColumns = @JoinColumn(name = "photo_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 20)
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(mappedBy = "likedPhotos")
    @BatchSize(size = 20)
    private Set<User> usersWhoLiked = new HashSet<>();

    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private Set<Comment> comments = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    // 【新增】反向映射相册 (mappedBy 指向 Album.photos)
    @ManyToMany(mappedBy = "photos")
    private Set<Album> albums = new HashSet<>();

    // 【核心修复】添加数据清洗辅助方法
    // 如果字符串是空的或全是空格，直接返回 null
    private String clean(String s) {
        if (s != null && s.trim().length() > 0) {
            return s;
        }
        return null;
    }

    // --- Getters 和 Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptionShort() {
        return descriptionShort;
    }

    public void setDescriptionShort(String description) {
        this.descriptionShort = description;
    }

    public String getDescriptionLong() {
        return descriptionLong;
    }

    public void setDescriptionLong(String descriptionLong) {
        this.descriptionLong = descriptionLong;
    }

    public String getCameraMake() {
        return clean(cameraMake);
    }

    public void setCameraMake(String cameraMake) {
        this.cameraMake = cameraMake;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getWebImageUrl() {
        return webImageUrl;
    }

    public void setWebImageUrl(String webImageUrl) {
        this.webImageUrl = webImageUrl;
    }

    public String getCameraModel() {
        return clean(cameraModel);
    }

    public void setCameraModel(String cameraModel) {
        this.cameraModel = cameraModel;
    }

    public String getAperture() {
        return clean(aperture);
    }

    public void setAperture(String aperture) {
        this.aperture = aperture;
    }

    public String getIso() {
        return clean(iso);
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getShutterSpeed() {
        return clean(shutterSpeed);
    }

    public void setShutterSpeed(String shutterSpeed) {
        this.shutterSpeed = shutterSpeed;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Set<User> getUsersWhoLiked() {
        return usersWhoLiked;
    }

    public void setUsersWhoLiked(Set<User> usersWhoLiked) {
        this.usersWhoLiked = usersWhoLiked;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }

    public int getLikeCount() {
        return usersWhoLiked.size();
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }

    // 地理位置
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // 色板
    public String getColorPalette() {
        return colorPalette;
    }

    public void setColorPalette(String colorPalette) {
        this.colorPalette = colorPalette;
    }

    public String getFocalLength() {
        return clean(focalLength);
    }

    public void setFocalLength(String focalLength) {
        this.focalLength = focalLength;
    }

    // Getter / Setter
    public Set<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(Set<Album> albums) {
        this.albums = albums;
    }

    public Float getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(Float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }

}