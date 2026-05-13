package com.yiliiii.project.my_photography_project.dto;
import com.yiliiii.project.my_photography_project.entity.Photo;


import java.time.LocalDate;
import java.util.List;

/**
 * 这是一个 DTO (数据传输对象)，用于在 Controller 和 Thymeleaf 之间
 * 传递一个“日期分组”的所有信息。
 */
public class PhotoGroupDto {
    
    // (例如: "2025年11月18日 星期二")
    private final String displayDate;
    
    // (例如: 2025-11-18，用于 API 调用)
    private final LocalDate isoDate;
    
    // (例如: "今天...")
    private String summary; 
    
    // (当天的照片列表)
    private final List<Photo> photos;
    
    // (如果为 true, Thymeleaf 将不会渲染 <h2> 和 <p>，只会渲染照片)
    private final boolean mergeWithPrevious;

    public PhotoGroupDto(String displayDate, LocalDate isoDate, List<Photo> photos, String summary, boolean mergeWithPrevious) {
        this.displayDate = displayDate;
        this.isoDate = isoDate;
        this.photos = photos;
        this.summary = summary;
        this.mergeWithPrevious = mergeWithPrevious;
    }

    // --- Getters ---
    
    public String getDisplayDate() { return displayDate; }
    public LocalDate getIsoDate() { return isoDate; }
    public String getSummary() { return summary; }
    public List<Photo> getPhotos() { return photos; }
    public boolean isMergeWithPrevious() { return mergeWithPrevious; }

    // --- Setter ---
    public void setSummary(String summary) { this.summary = summary; }
}