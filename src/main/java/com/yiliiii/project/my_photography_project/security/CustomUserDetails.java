package com.yiliiii.project.my_photography_project.security;
import com.yiliiii.project.my_photography_project.config.SecurityConfig;
import com.yiliiii.project.my_photography_project.entity.User;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 自定义的用户详情包装类
 * 目的：让 Spring Security 的 Principal 能访问到 avatarUrl
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // 【关键】暴露头像 URL 给前端 Thymeleaf 使用
    public String getAvatarUrl() {
        return user.getAvatarUrl();
    }

    // 【新增】暴露 ID 给 SecurityConfig 使用
    public Long getId() {
        return user.getId();
    }

    // --- 以下是 UserDetails 必须实现的方法 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}