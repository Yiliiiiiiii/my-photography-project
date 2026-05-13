package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.entity.DailySummary;
import com.yiliiii.project.my_photography_project.service.DailySummaryService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/summary")
public class DailySummaryApiController {

    @Autowired
    private DailySummaryService summaryService;

    /**
     * 处理保存/更新/删除每日总结的 AJAX 请求
     * @param date "ISO" 格式 (yyyy-MM-dd)
     * @param content 总结内容 (如果为空，则删除)
     */
    @PostMapping("/{date}")
    public ResponseEntity<?> saveSummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "content", required = false) String content) {
        
        try {
            DailySummary savedSummary = summaryService.saveOrUpdateSummary(date, content);
            
            if (savedSummary != null) {
                // 成功创建或更新
                return ResponseEntity.ok(Map.of("content", savedSummary.getContent()));
            } else {
                // 成功删除 (内容为空)
                return ResponseEntity.ok(Map.of("content", "")); // 返回空内容
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("保存失败: " + e.getMessage());
        }
    }
}