package com.yiliiii.project.my_photography_project.service;
import com.yiliiii.project.my_photography_project.entity.Notification;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.NotificationRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 创建并发送通知
     */
    @Transactional
    public void createAndSend(User recipient, User actor, Photo photo, String type, String content) {

        // 1. 自己不给自己发通知
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        // 2. 存入数据库
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setActor(actor);
        n.setPhoto(photo);
        n.setType(type); // "LIKE", "COMMENT", "REPLY"
        n.setContent(content);
        notificationRepository.save(n);

        // 3. 构建推送消息
        // 【关键修复】只定义一次 messageText
        String messageText = "";

        if ("LIKE".equals(type)) {
            messageText = actor.getUsername() + " 赞了你的作品《" + photo.getTitle() + "》";
        } else if ("COMMENT".equals(type)) {
            messageText = actor.getUsername() + " 评论了你的作品《" + photo.getTitle() + "》";
        } else if ("REPLY".equals(type)) {
            // 【新增的 REPLY 逻辑放在这里】
            messageText = actor.getUsername() + " 回复了你的评论";
        }

        // 4. 发送 WebSocket (实时弹窗用)
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getUsername(),
                    "/queue/notifications",
                    Map.of(
                            "message", messageText,
                            "photoId", photo.getId(),
                            "type", type));
        } catch (Exception e) {
            System.err.println("WS 发送失败: " + e.getMessage());
        }
    }

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> list = notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
        for (Notification n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
    }
}