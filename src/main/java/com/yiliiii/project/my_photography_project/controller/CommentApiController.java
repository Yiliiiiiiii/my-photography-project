package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.config.SecurityConfig;
import com.yiliiii.project.my_photography_project.dto.CommentDto;
import com.yiliiii.project.my_photography_project.service.CommentService;
 // 1. 你的包名

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 评论 API 控制器
 */
@RestController
@RequestMapping("/api")
public class CommentApiController {

    @Autowired
    private CommentService commentService;

    /**
     * 【已修复】: 处理 "发布评论" 的 AJAX 请求
     * (增加了 parentId 参数以支持回复功能)
     */
    @PostMapping("/comment/{photoId}")
    public ResponseEntity<?> submitComment(@PathVariable Long photoId,
                                           @RequestParam("content") String content,
                                           @RequestParam(value = "parentId", required = false) Long parentId, // <--- 【新增参数】
                                           Authentication auth) {
        
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(403).body("未登录"); // 403 Forbidden
        }
        
        try {
            String username = auth.getName();
            
            // 1. 【修复】: 调用带 parentId 的新 Service 方法
            CommentDto savedCommentDto = commentService.postComment(photoId, content, username, parentId);
            
            // 2. 直接返回 DTO, Spring 会把它转为 JSON
            return ResponseEntity.ok(savedCommentDto);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage()); // 404 Not Found
        }
    }

    /**
     * 处理 "删除评论" 的 AJAX 请求
     * (需要 ADMIN 权限 - 我们将在 SecurityConfig 中设置)
     */
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId);
            // 204 No Content 是 DELETE 成功的标准响应
            return ResponseEntity.noContent().build(); 
        } catch (IllegalArgumentException e) {
            // 404 Not Found (如果 ID 无效)
            return ResponseEntity.notFound().build();
        }
    }
}