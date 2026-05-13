package com.yiliiii.project.my_photography_project.dto;
import com.yiliiii.project.my_photography_project.entity.Comment;
 // 1. 你的包名

import java.time.LocalDateTime;

public class CommentDto {
    
    // 【【【新增字段】】】
    private Long id; 
    
    private String username;
    private String content;
    private LocalDateTime createdAt;
    
    // 【【【新增】】】
    private String avatarUrl;

    // 【【【新增字段】】】
    private Long parentId;          // 父评论 ID
    private String parentUsername;  // 被回复的人的名字

    // 构造函数
    public CommentDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        
        // (之前的 null 检查 - 保持不变)
        if (comment.getUser() != null) {
            this.username = comment.getUser().getUsername();
            
            // 【【【修改点】】】
            String userAvatar = comment.getUser().getAvatarUrl();
            if (userAvatar != null && !userAvatar.isEmpty()) {
                this.avatarUrl = userAvatar;
            } else {
                // 如果数据库中是 null 或空字符串，使用默认头像
                this.avatarUrl = "/images/default_avatar.png"; 
            }

        } else {
            this.username = "未知用户";
            this.avatarUrl = "/images/default_avatar.png";
        }

        // 【【【新增逻辑：处理父评论信息】】】
        if (comment.getParent() != null) {
            this.parentId = comment.getParent().getId();
            // 注意：这里 parent.getUser() 可能会触发 Lazy Loading，
            // 但因为我们在 Service 事务中，或者是 N+1 优化过的查询，所以通常没问题。
            // 为了保险，确保 parent.getUser() 不为空
            if (comment.getParent().getUser() != null) {
                this.parentUsername = comment.getParent().getUser().getUsername();
            } else {
                this.parentUsername = "未知用户";
            }
        }

    }
    
    // --- Getters ---
    
    // 【【【新增 Getter】】】
    public Long getId() { return id; }

    public String getUsername() { return username; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // 【【【新增 Getter】】】
    public String getAvatarUrl() { return avatarUrl; }


    // 【【【新增 Getters】】】
    public Long getParentId() { return parentId; }
    public String getParentUsername() { return parentUsername; }

    
}