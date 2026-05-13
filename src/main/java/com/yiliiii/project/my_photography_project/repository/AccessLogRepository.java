package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.AccessLog;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    // 【修改】不再限制数量，获取全部日志，按时间倒序
    List<AccessLog> findAllByOrderByVisitTimeDesc();

    // 【新增】获取不等于指定用户名的日志
    List<AccessLog> findByUsernameNotOrderByVisitTimeDesc(String username);

    /**
     * 【新增】核心合并逻辑
     * 找到所有属于该 visitorId 且用户名为 "Guest" 的记录，更新为真实用户名
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE AccessLog l SET l.username = :newUsername WHERE l.visitorId = :visitorId AND l.username = 'Guest'")
    void mergeGuestLogs(@org.springframework.data.repository.query.Param("visitorId") String visitorId,
            @org.springframework.data.repository.query.Param("newUsername") String newUsername);

}