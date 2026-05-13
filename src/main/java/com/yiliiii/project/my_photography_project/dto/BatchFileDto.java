package com.yiliiii.project.my_photography_project.dto;

/**
 * 批量文件数据传输对象
 * 这是一个线程安全的数据容器，用于将文件内容从
 * Controller 线程传递到 Service 的 Async 线程。
 */
public class BatchFileDto {
    
    private final byte[] content;
    private final String originalFilename;

    public BatchFileDto(byte[] content, String originalFilename) {
        this.content = content;
        this.originalFilename = originalFilename;
    }

    public byte[] getContent() {
        return content;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }
}