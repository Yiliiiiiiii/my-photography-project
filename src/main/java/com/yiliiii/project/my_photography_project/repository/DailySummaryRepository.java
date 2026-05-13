package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.DailySummary;


import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    // 通过单个日期查找
    Optional<DailySummary> findByDate(LocalDate date);

    // 通过日期列表一次性查找
    List<DailySummary> findByDateIn(List<LocalDate> dates);
}