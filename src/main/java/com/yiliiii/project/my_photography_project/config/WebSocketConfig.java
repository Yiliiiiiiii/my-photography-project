package com.yiliiii.project.my_photography_project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 1. 开启 STOMP 协议的 WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@SuppressWarnings("null") MessageBrokerRegistry config) {
        // 2. 启用简单的内存消息代理
        // /topic: 用于广播消息 (大家都能收到)
        // /queue: 用于点对点消息 (只有特定用户能收到，例如 "你的照片被评论了")
        config.enableSimpleBroker("/topic", "/queue");

        // 客户端发送消息的前缀 (本例中我们主要只做"服务器推送到客户端"，这个暂时用不到)
        config.setApplicationDestinationPrefixes("/app");
    }

    @SuppressWarnings("null")
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. 注册一个 WebSocket 端点，前端将连接这个 URL
        registry.addEndpoint("/ws-connect")
                .setAllowedOriginPatterns("*") // 允许跨域 (防止开发环境连接失败)
                .withSockJS(); // 开启 SockJS 回退机制 (如果浏览器不支持 WebSocket，自动退化为轮询)
    }
}