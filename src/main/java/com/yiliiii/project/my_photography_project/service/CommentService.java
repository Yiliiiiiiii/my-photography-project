package com.yiliiii.project.my_photography_project.service;
import com.yiliiii.project.my_photography_project.dto.CommentDto;
import com.yiliiii.project.my_photography_project.entity.Comment;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.CommentRepository;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;
import com.yiliiii.project.my_photography_project.repository.UserRepository;
 // 1. 你的包名

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Nonnull;

@Service
@SuppressWarnings("null")
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PhotoRepository photoRepository;

    // 【关键修改】注入 NotificationService (而不是直接注入 SimpMessagingTemplate)
    @Autowired
    private NotificationService notificationService;

    /**
     * 发布评论 (支持回复)
     * 【关键修复】参数列表必须包含 Long parentId
     */
    @Transactional
    public CommentDto postComment(@Nonnull Long photoId, String content, String username, Long parentId) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("无效的用户名: " + username));

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("无效的照片ID: " + photoId));

        Comment newComment = new Comment();
        newComment.setContent(content);
        newComment.setUser(user);
        newComment.setPhoto(photo);

        // 处理父评论
        User parentCommentAuthor = null;
        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的父评论ID"));
            newComment.setParent(parentComment);
            parentCommentAuthor = parentComment.getUser();
        }

        Comment savedComment = commentRepository.save(newComment);

        // ---------------------------------------------------------
        // 【通知逻辑】调用 NotificationService
        // ---------------------------------------------------------
        User photoOwner = photo.getUploader();

        // 如果照片有主人，且评论者不是主人自己
        if (photoOwner != null && !photoOwner.getUsername().equals(username)) {
            // 创建一条类型为 "COMMENT" 的通知
            notificationService.createAndSend(photoOwner, user, photo, "COMMENT", content);
        }

        // 2. 【新增】通知被回复的人 (如果存在父评论，且被回复的人不是评论者自己)
        if (parentCommentAuthor != null && !parentCommentAuthor.getUsername().equals(username)) {
            // 为了防止重复打扰：如果照片主人就是被回复的人，上面已经通知了，这里就不需要发 "REPLY" 了？
            // 通常策略：都发，或者区分开。为了简单清晰，我们发一条专门的 REPLY 通知。

            if (!parentCommentAuthor.getId().equals(photoOwner.getId())) {
                // 只有当 被回复者 != 照片主人 时，才单独发一条 REPLY 通知
                // (否则照片主人会收到两条：一条"评论了作品"，一条"回复了评论")
                notificationService.createAndSend(parentCommentAuthor, user, photo, "REPLY", content);
            }
        }

        // ---------------------------------------------------------

        // 4. 返回 DTO
        return new CommentDto(savedComment);
    }

    /**
     * 删除评论 (管理员)
     */
    @Transactional
    public void deleteComment(@Nonnull Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("无效的评论ID: " + commentId));

        commentRepository.delete(comment);
    }
}