package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.service.UserService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistrationController {

    @Autowired
    private UserService userService;

    /**
     * 【【【【【【新增的方法】】】】】】
     * 显示登录页面
     * (这就是修复错误所需要的)
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // <-- 返回 templates/login.html
    }

    /**
     * 显示注册页面
     */
    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register"; // templates/register.html
    }

    /**
     * 处理注册请求
     */
    @PostMapping("/register")
    public String processRegistration(@RequestParam("username") String username,
            @RequestParam("password") String password,
            Model model) {

        boolean success = userService.registerNewVisitor(username, password);

        if (success) {
            // 注册成功, 重定向到登录页, 并带上一个 "成功" 提示
            return "redirect:/login?registered";
        } else {
            // 注册失败 (用户名已存在)
            model.addAttribute("error", "用户名已被占用");
            return "register"; // 留在注册页, 并显示错误
        }

    }
}