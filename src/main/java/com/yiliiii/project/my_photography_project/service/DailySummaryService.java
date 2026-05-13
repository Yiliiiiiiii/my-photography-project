package com.yiliiii.project.my_photography_project.service;
import com.yiliiii.project.my_photography_project.entity.DailySummary;
import com.yiliiii.project.my_photography_project.repository.DailySummaryRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class DailySummaryService {

    @Autowired
    private DailySummaryRepository summaryRepository;

    /**
     * 为一组日期获取对应的总结，返回一个 Map
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, String> getSummariesForDates(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return Map.of();
        }

        // 批量查询
        return summaryRepository.findByDateIn(dates).stream()
                .collect(Collectors.toMap(DailySummary::getDate, DailySummary::getContent));
    }

    /**
     * 保存或更新一个日期的总结
     * (如果 content 为空或 null，则删除该日期的总结)
     */
    @Transactional
    public DailySummary saveOrUpdateSummary(LocalDate date, String content) {

        Optional<DailySummary> existingSummaryOpt = summaryRepository.findByDate(date);

        // 逻辑1：如果内容为空，则删除
        if (content == null || content.trim().isEmpty()) {
            if (existingSummaryOpt.isPresent()) {
                summaryRepository.delete(existingSummaryOpt.get());
            }
            return null; // 表示已删除
        }

        // 逻辑2：内容不为空，则创建或更新
        DailySummary summary;
        if (existingSummaryOpt.isPresent()) {
            summary = existingSummaryOpt.get();
            summary.setContent(content); // 更新内容
        } else {
            summary = new DailySummary(date, content); // 创建新的
        }

        return summaryRepository.save(summary);
    }
}