package com.yiliiii.project.my_photography_project.entity; // 1. 你的包名

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import java.time.LocalDateTime;

// 【【【新增导入】】】
import java.time.ZoneOffset;
import jakarta.persistence.PrePersist;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 【【【【【【修改点 1】】】】】】
    // 移除 "insertable = false" 和 "columnDefinition"
    // 这允许 Java (JPA) 来设置这个值
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // --- 关系 ---
    // (User 和 Photo 的关系保持不变)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    // 【【【新增：父评论 (回复了哪条评论)】】】
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 【【【新增：子评论列表 (可选，用于级联删除)】】】
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();
    // 【【【【【【修改点 2】】】】】】
    // 添加一个新的方法, 使用 @PrePersist 注解
    // 这会在 JpaRepository.save() 执行 *之前* 自动运行
    @PrePersist
    protected void onCreate() {
        // 强制将时间设置为当前的 UTC 时间
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }


    // --- Getters and Setters ---
    // (所有 Getters 和 Setters 保持不变)
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Photo getPhoto() { return photo; }
    public void setPhoto(Photo photo) { this.photo = photo; }

// 【【【新增 Getters/Setters】】】
    public Comment getParent() { return parent; }
    public void setParent(Comment parent) { this.parent = parent; }
    public List<Comment> getReplies() { return replies; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }



}