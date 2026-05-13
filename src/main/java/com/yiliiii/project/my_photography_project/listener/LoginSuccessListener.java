package com.yiliiii.project.my_photography_project.listener;
import com.yiliiii.project.my_photography_project.repository.AccessLogRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
// 【1. 新增导入】
import org.springframework.lang.NonNull;

@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Autowired
    private AccessLogRepository logRepository;

    @Override

    public void onApplicationEvent(@NonNull AuthenticationSuccessEvent event) {
        // 1. 获取登录用户名
        UserDetails user = (UserDetails) event.getAuthentication().getPrincipal();
        String username = user.getUsername();

        // 2. 获取当前请求中的 Cookie
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            HttpServletRequest request = attr.getRequest();
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("VISITOR_ID".equals(cookie.getName())) {
                        // 3. 执行数据库合并操作
                        logRepository.mergeGuestLogs(cookie.getValue(), username);
                        System.out.println(">>> 已将访客 [" + cookie.getValue() + "] 的历史记录合并给用户: " + username);
                        break;
                    }
                }
            }
        }
    }
}