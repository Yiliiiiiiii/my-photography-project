package com.yiliiii.project.my_photography_project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "album")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    private LocalDateTime createdAt;

    // 封面图 URL (可选，默认用第一张图)
    private String coverImageUrl;

    // --- 关系 ---
    
    // 相册属于某个用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 多对多：一个相册有多张照片，一张照片可以属于多个相册
    @ManyToMany
    @JoinTable(
        name = "album_photos",
        joinColumns = @JoinColumn(name = "album_id"),
        inverseJoinColumns = @JoinColumn(name = "photo_id")
    )
    @OrderBy("takenAt DESC") // 相册内照片按拍摄时间倒序
    private Set<Photo> photos = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Set<Photo> getPhotos() { return photos; }
    public void setPhotos(Set<Photo> photos) { this.photos = photos; }
    
    // 辅助：获取照片数量
    public int getPhotoCount() {
        return photos.size();
    }
}