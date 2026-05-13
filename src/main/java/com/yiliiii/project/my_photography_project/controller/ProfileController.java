package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.UserRepository;
import com.yiliiii.project.my_photography_project.security.CustomUserDetails;
import com.yiliiii.project.my_photography_project.service.PhotoService;
import com.yiliiii.project.my_photography_project.service.UserService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// (我们不再需要 PhotoService 的导入了，因为 savePhoto 方法已被移除)
// import com.yiliiii.project.my_photography_project.PhotoService;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // (我们不再需要 photoService 字段，因为 savePhoto 方法已被移除)
    // @Autowired
    // private PhotoService photoService;

    /**
     * 1. 显示个人资料页面
     */
    @GetMapping("/profile")
    public String showProfilePage(Model model, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("无效的用户"));

        model.addAttribute("user", currentUser);
        return "profile";
    }

    /**
     * 2. 处理头像上传 (修复版：支持立即刷新右上角头像)
     */
    @PostMapping("/profile/avatar-upload")
    public String handleAvatarUpload(@RequestParam("avatarFile") MultipartFile avatarFile,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        if (auth == null)
            return "redirect:/login";

        if (avatarFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "请选择一个文件。");
            return "redirect:/profile";
        }

        try {
            // 1. 执行物理保存和数据库更新 (这一步你已经做好了)
            userService.updateUserAvatar(auth.getName(), avatarFile);

            // =====================================================
            // 【核心修复】手动刷新 Spring Security 的上下文 (Session)
            // =====================================================

            // 2.1 从数据库重新查出最新的 User 对象 (此时它包含了新的 avatarUrl)
            User updatedUser = userRepository.findByUsername(auth.getName()).orElseThrow();

            // 2.2 用最新的 User 对象，重新包装一个新的“身份证” (Principal)
            UserDetails newPrincipal = new CustomUserDetails(updatedUser);

            // 2.3 生成新的认证令牌 (保持原有的密码凭证和权限)
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    newPrincipal,
                    auth.getCredentials(),
                    newPrincipal.getAuthorities());

            // 2.4 强行塞回安全上下文，覆盖掉旧的
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            // =====================================================

            redirectAttributes.addFlashAttribute("success", "头像更新成功！");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "头像上传失败：" + e.getMessage());
        }

        return "redirect:/profile";
    }

    /**
     * 3. 处理用户名更改
     */
    @PostMapping("/profile/update-username")
    public String handleUsernameUpdate(@RequestParam("newUsername") String newUsername,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (auth == null) {
            return "redirect:/login";
        }

        try {
            boolean success = userService.changeUsername(auth.getName(), newUsername);

            if (success) {
                redirectAttributes.addFlashAttribute("success", "用户名更新成功！请使用新用户名登录。");
                new SecurityContextLogoutHandler().logout(request, response, auth);
                return "redirect:/login";

            } else {
                redirectAttributes.addFlashAttribute("error", "用户名更新失败：该用户名已被占用。");
                return "redirect:/profile";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
            return "redirect:/profile";
        }
    }

    /**
     * 4. 处理密码更改
     */
    @PostMapping("/profile/update-password")
    public String handlePasswordUpdate(@RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (auth == null) {
            return "redirect:/login";
        }

        try {
            boolean success = userService.changePassword(auth.getName(), oldPassword, newPassword);

            if (success) {
                redirectAttributes.addFlashAttribute("success", "密码更新成功！请使用新密码登录。");
                new SecurityContextLogoutHandler().logout(request, response, auth);
                return "redirect:/login";

            } else {
                redirectAttributes.addFlashAttribute("error", "密码更新失败：旧密码不正确。");
                return "redirect:/profile";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
            return "redirect:/profile";
        }
    }

    // 【【【【【【关键修复】】】】】】
    // 删除了这里重复的 savePhoto(...) 方法。
    // 【【【【【【修复结束】】】】】】

}