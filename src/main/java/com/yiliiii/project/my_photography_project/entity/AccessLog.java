package com.yiliiii.project.my_photography_project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat; // 【新增导入】

@Entity
@Table(name = "access_log")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // 登录名 (游客显示 "Guest")
    private String ipAddress; // 访客 IP
    private String url; // 访问了哪个页面
    private String method; // GET / POST
    private String userAgent; // 设备信息 (如 iPhone / Chrome)

    // 【新增】地理位置 (e.g. "中国 浙江 杭州")
    private String location;

    // 【新增】访客追踪 ID (存储 Cookie 中的 UUID)
    private String visitorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime visitTime = LocalDateTime.now();

    // --- Constructors, Getters, Setters ---
    public AccessLog() {
    }

    public AccessLog(String username, String ip, String url, String method, String ua, String visitorId) {
        this.username = username;
        this.ipAddress = ip;
        this.url = url;
        this.method = method;
        this.userAgent = ua;
        this.visitorId = visitorId; // 【新增】
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getVisitTime() {
        return visitTime;
    }

    // Getter & Setter
    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    // 【新增】Getter / Setter
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}