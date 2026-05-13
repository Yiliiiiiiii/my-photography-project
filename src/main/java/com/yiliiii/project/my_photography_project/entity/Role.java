package com.yiliiii.project.my_photography_project.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles") // 定义数据库中的表名
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 角色名, 例如 "ROLE_ADMIN" 或 "ROLE_USER"
    // 必须是唯一的
    @Column(nullable = false, unique = true)
    private String name;

    // "mappedBy = roles" 告诉 JPA：
    // "去 User.java 实体里找 'roles' 字段，那里的 @JoinTable 定义了关系"
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    // --- 构造函数, Getters, Setters ---
    public Role() {}
    
    public Role(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }
}