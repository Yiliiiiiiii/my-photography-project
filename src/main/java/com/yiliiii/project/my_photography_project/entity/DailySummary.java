package com.yiliiii.project.my_photography_project.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_summary")
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关键字段：日期，必须唯一
    @Column(nullable = false, unique = true, name = "summary_date")
    private LocalDate date;

    // 总结内容，使用 TEXT 类型
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 构造函数
    public DailySummary() {}

    public DailySummary(LocalDate date, String content) {
        this.date = date;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}