package com.yiliiii.project.my_photography_project.interceptor;
import com.yiliiii.project.my_photography_project.entity.AccessLog;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.AccessLogRepository;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    @Autowired
    private AccessLogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        String uri = request.getRequestURI();

        // 1. 过滤静态资源
        if (uri.startsWith("/css") || uri.startsWith("/js") ||
                uri.startsWith("/images") || uri.startsWith("/uploads") ||
                uri.contains("favicon") || uri.startsWith("/ws-connect")) {
            return true;
        }

        // 2. Visitor ID
        String visitorId = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("VISITOR_ID".equals(cookie.getName())) {
                    visitorId = cookie.getValue();
                    break;
                }
            }
        }
        if (visitorId == null) {
            visitorId = UUID.randomUUID().toString();
            Cookie newCookie = new Cookie("VISITOR_ID", visitorId);
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 60 * 24 * 365);
            newCookie.setHttpOnly(true);
            response.addCookie(newCookie);
        }

        // 3. User
        String username = "Guest";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!"anonymousUser".equals(name))
                username = name;
        }

        // 4. IP 处理 (临时变量)
        String tempIp = request.getHeader("X-Forwarded-For");
        if (tempIp == null || tempIp.isEmpty() || "unknown".equalsIgnoreCase(tempIp)) {
            tempIp = request.getRemoteAddr();
        }
        if (tempIp != null && tempIp.contains(",")) {
            tempIp = tempIp.split(",")[0].trim();
        }

        // 【核心修复】将处理好的 IP 赋值给一个新的、不会再被修改的变量
        // 这样它就是 effectively final 的，可以安全传入 Lambda 表达式
        String finalIp = tempIp;

        // 5. Device
        String ua = request.getHeader("User-Agent");
        String device = "Unknown";
        if (ua != null) {
            if (ua.contains("Mobile"))
                device = "📱 Mobile";
            else if (ua.contains("Windows"))
                device = "💻 Windows";
            else if (ua.contains("Mac"))
                device = "🍎 Mac";
            else if (ua.contains("Linux"))
                device = "🐧 Linux";
            else
                device = "🖥️ PC";
        }

        // 6. 先保存基础日志
        AccessLog log = new AccessLog(username, finalIp, uri, request.getMethod(), device, visitorId);
        AccessLog savedLog = logRepository.save(log);

        // 7. 异步查询 IP 归属地 (使用 finalIp)
        if (finalIp != null && !finalIp.equals("127.0.0.1") && !finalIp.equals("0:0:0:0:0:0:0:1")
                && !finalIp.startsWith("192.168")) {
            CompletableFuture.runAsync(() -> {
                try {
                    // 使用 ip-api.com
                    String apiUrl = "http://ip-api.com/json/" + finalIp + "?lang=zh-CN";

                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder responseStr = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null)
                            responseStr.append(inputLine);
                        in.close();

                        JsonNode root = objectMapper.readTree(responseStr.toString());
                        if ("success".equals(root.path("status").asText())) {
                            String region = root.path("regionName").asText();
                            String city = root.path("city").asText();

                            // 更新数据库
                            savedLog.setLocation(region + " " + city);
                            logRepository.save(savedLog);
                        }
                    }
                } catch (Exception e) {
                    // 忽略查询失败
                }
            });
        } else {
            savedLog.setLocation("内网 IP");
            logRepository.save(savedLog);
        }

        return true;
    }
}