package com.yiliiii.project.my_photography_project.entity; // 1. 你的包名

// 确保导入 FetchType
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // 【【【新增】】】: 头像 URL 字段
    @Column(name = "avatar_url")
    private String avatarUrl;

    // --- 关系 ---

    // 【【【修改点 1】】】: "String roles" 已被 "Set<Role> roles" 替换
    @ManyToMany(fetch = FetchType.EAGER) // 必须是 EAGER 才能用于 Security
    @JoinTable(
        name = "users_roles", // 中间表的名字
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    
    // (点赞关系 - 保持不变)
    @ManyToMany
    @JoinTable(
        name = "photo_likes",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "photo_id")
    )
    private Set<Photo> likedPhotos = new HashSet<>();
    
    // (评论关系 - 保持不变)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();


    // 【【【新增】】】: 该用户上传的所有照片
    @OneToMany(mappedBy = "uploader", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Photo> uploadedPhotos = new HashSet<>();

    // --- Getters and Setters ---
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // 【【【新增】】】: avatarUrl 的 Getters/Setters
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    // 【【【修改点 2】】】: roles 的 Getters/Setters (类型已改为 Set<Role>)
    public Set<Role> getRoles() { 
        return this.roles; 
    }
    public void setRoles(Set<Role> roles) { 
        this.roles = roles; 
    }

    // (likedPhotos 的 Getters/Setters - 保持不变)
    public Set<Photo> getLikedPhotos() { return likedPhotos; }
    public void setLikedPhotos(Set<Photo> likedPhotos) { this.likedPhotos = likedPhotos; }
    
    // (comments 的 Getters/Setters - 保持不变)
    public Set<Comment> getComments() { return comments; }
    public void setComments(Set<Comment> comments) { this.comments = comments; }

    // 【【【新增】】】: 用于 profile.html 的辅助方法
    public boolean hasRole(String roleName) {
        // 遍历 this.roles 集合
        for (Role role : this.roles) { 
            if (role.getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    // 【【【新增】】】: uploadedPhotos 的 Getter/Setter
    public Set<Photo> getUploadedPhotos() { return uploadedPhotos; }
    public void setUploadedPhotos(Set<Photo> uploadedPhotos) { this.uploadedPhotos = uploadedPhotos; }
    
}