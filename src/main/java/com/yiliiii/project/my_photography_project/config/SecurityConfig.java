package com.yiliiii.project.my_photography_project.config;

import com.yiliiii.project.my_photography_project.security.CustomUserDetails;
import com.yiliiii.project.my_photography_project.security.JpaUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JpaUserDetailsService jpaUserDetailsService;

        public SecurityConfig(JpaUserDetailsService jpaUserDetailsService) {
                this.jpaUserDetailsService = jpaUserDetailsService;
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // 1. 公共访问区
                                                .requestMatchers(
                                                                "/", "/gallery", "/gallery/3d", "/wall", "/map", "/error", "/login",
                                                                "/register",
                                                                "/immersive/**",
                                                                "/uploads/**", "/images/**", "/css/**", "/js/**",
                                                                "/favicon.ico", "/api/stats", "/api/map-data",
                                                                "/ws-connect/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/photo/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/photos/simple").permitAll()

                                                // 2. 基础登录用户权限
                                                .requestMatchers(HttpMethod.POST, "/like/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/comment/**").authenticated()
                                                .requestMatchers("/profile/**").authenticated()

                                                // ============================================================
                                                // 3. 【普通管理员权限】 (ROLE_ADMIN) - 负责内容运营
                                                // ============================================================
                                                // 允许普通管理员进行：发布、编辑、删除、批量导入、AI生成、写总结
                                                .requestMatchers(
                                                                "/add", "/save", // 发布
                                                                "/edit/**", "/update", // 编辑
                                                                "/delete/**", // 删除 (页面跳转)
                                                                "/admin/batch-upload", // 批量导入页面
                                                                "/api/summary/**" // 每日总结 API
                                                ).hasRole("ADMIN")

                                                // 允许普通管理员调用特殊 API
                                                .requestMatchers(HttpMethod.POST, "/api/photo/{id}/generate-ai")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/photo/**", "/api/comment/**")
                                                .hasRole("ADMIN")

                                                // ============================================================
                                                // 4. 【超级管理员专属】 (ID = 1) - 负责人员管理
                                                // ============================================================
                                                // 仅限 ID=1 的用户访问用户管理列表、修改他人权限
                                                .requestMatchers(
                                                                "/admin/users/**", // 用户列表页面
                                                                "/admin/users/edit/**", "/admin/users/update",
                                                                "/admin/users/delete/**", // 用户操作
                                                                "/admin/backfill-exif" // 维护工具也建议留给超管，或者放开给 ADMIN 也可以
                                                ).access((authentication, object) -> {
                                                        if (authentication.get()
                                                                        .getPrincipal() instanceof CustomUserDetails user) {
                                                                return new AuthorizationDecision(
                                                                                user.getId().equals(1L));
                                                        }
                                                        return new AuthorizationDecision(false);
                                                })

                                                .anyRequest().authenticated())
                                .userDetailsService(jpaUserDetailsService)
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/gallery", true)
                                                .permitAll())
                                // 【新增】记住我功能 (2周免登录)
                                .rememberMe(remember -> remember
                                                .userDetailsService(jpaUserDetailsService)
                                                .tokenValiditySeconds(60 * 60 * 24 * 14) // 14天
                                                .key("photography-project-secret-key"))
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/")
                                                .permitAll());

                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
