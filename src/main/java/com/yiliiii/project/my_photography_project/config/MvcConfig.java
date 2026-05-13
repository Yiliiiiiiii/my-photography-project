package com.yiliiii.project.my_photography_project.config;
import com.yiliiii.project.my_photography_project.interceptor.AccessLogInterceptor;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Paths;
import org.springframework.lang.NonNull;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${myapp.upload-dir}")
    private String uploadDir;
    // 【新增】注入拦截器
    @Autowired
    private AccessLogInterceptor accessLogInterceptor;

    @SuppressWarnings("null")
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 获取绝对路径的 URI 字符串 (例如: file:///www/wwwroot/project)
        String resourceLocation = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        // 2. 【关键修复】Linux 下必须以斜杠结尾，否则 Spring 无法识别为目录
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        System.out.println(">>> [MvcConfig] 静态资源映射路径: " + resourceLocation);

        // 3. 配置映射
        // 访问 /uploads/** -> 去 resourceLocation 找
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }

    // 【新增】注册拦截器
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/**"); // 拦截所有路径 (逻辑里会过滤静态资源)
    }
}