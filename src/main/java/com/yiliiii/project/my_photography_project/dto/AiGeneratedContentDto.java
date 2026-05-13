package com.yiliiii.project.my_photography_project.dto;

// 一个简单的 POJO (或 Java 17+ Record)
public class AiGeneratedContentDto {
    private String title;
    private String descriptionLong;

    // 构造函数、Getters、Setters...
    public AiGeneratedContentDto(String title, String descriptionLong) {
        this.title = title;
        this.descriptionLong = descriptionLong;
    }
    
    public String getTitle() { return title; }
    public String getDescriptionLong() { return descriptionLong; }
}