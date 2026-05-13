package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.Notification;
import com.yiliiii.project.my_photography_project.entity.User;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 查找发给某个用户的通知，按时间倒序
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    
    // (可选) 统计未读数量
    long countByRecipientAndIsReadFalse(User recipient);
}