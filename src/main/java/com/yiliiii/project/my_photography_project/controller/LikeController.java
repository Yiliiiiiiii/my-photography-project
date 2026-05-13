package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.service.PhotoService;
 // 1. 你的包名

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map; // 导入 Map

@RestController
public class LikeController {

    @Autowired
    private PhotoService photoService;

    @PostMapping("/like/{photoId}")
    public Map<String, Object> toggleLike(@PathVariable Long photoId, Authentication auth) {
        
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return Map.of("error", "未登录");
        }

        String username = auth.getName(); 
        int newLikeCount = photoService.toggleLike(photoId, username);
        
        // 返回一个 JSON 对象 (e.g., { "likeCount": 15 })
        return Map.of("likeCount", newLikeCount);
    }
}