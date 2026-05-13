package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.UserRepository;
import com.yiliiii.project.my_photography_project.service.NotificationService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/notifications")
    public String showNotifications(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";
        
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow();
        
        // 获取所有通知
        model.addAttribute("notifications", notificationService.getUserNotifications(user));
        
        // (可选) 访问页面即视为已读
        notificationService.markAllAsRead(user);
        
        return "notifications"; // 返回 notifications.html
    }
}