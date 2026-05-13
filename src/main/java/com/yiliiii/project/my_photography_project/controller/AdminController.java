package com.yiliiii.project.my_photography_project.controller;

import com.yiliiii.project.my_photography_project.dto.BatchFileDto;
import com.yiliiii.project.my_photography_project.entity.AccessLog;
import com.yiliiii.project.my_photography_project.entity.Role;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.AccessLogRepository;
import com.yiliiii.project.my_photography_project.service.PhotoService;
import com.yiliiii.project.my_photography_project.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.io.IOException;

@Controller
@RequestMapping("/admin") // 锁定该控制器的所有路径都在 /admin 之下
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private PhotoService photoService; // (这个已存在)

    @Autowired
    private AccessLogRepository accessLogRepository;

    // ... (showBatchUploadForm, handleBatchUpload 保持不变) ...
    @GetMapping("/batch-upload")
    public String showBatchUploadForm() {
        return "admin-batch-upload"; // <-- 指向新创建的 admin-batch-upload.html
    }

    @PostMapping("/batch-upload")
    public String handleBatchUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "generateAi", defaultValue = "false") boolean generateAi,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        if (files.isEmpty() || files.get(0).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "您必须至少选择一个文件。");
            return "redirect:/admin/batch-upload";
        }

        List<BatchFileDto> fileDtos = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    fileDtos.add(new BatchFileDto(file.getBytes(), file.getOriginalFilename()));
                }
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "读取文件时出错: " + e.getMessage());
            return "redirect:/admin/batch-upload";
        }

        photoService.batchImportPhotos(fileDtos, generateAi, auth.getName());

        redirectAttributes.addFlashAttribute("success",
                "批量导入任务已开始 (共 " + fileDtos.size() + " 个文件)。这可能需要几分钟时间。");

        return "redirect:/admin/batch-upload";
    }

    // ... (showUserManagementPanel, showEditUserForm, updateUserRoles, deleteUser
    // 保持不变) ...
    @GetMapping("/users")
    public String showUserManagementPanel(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin-panel"; // <-- 返回 admin-panel.html
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        List<Role> allRoles = userService.getAllRoles();

        model.addAttribute("user", user);
        model.addAttribute("allRoles", allRoles);

        return "admin-edit-user"; // <-- 返回 admin-edit-user.html
    }

    @PostMapping("/users/update")
    public String updateUserRoles(@RequestParam("userId") Long userId,
            @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRoles(userId, roleIds);
            redirectAttributes.addFlashAttribute("success", "用户角色更新成功。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id, auth.getName());
            redirectAttributes.addFlashAttribute("success", "用户删除成功。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * 【【【【【【修改点：用于触发“拍摄时间”回填的端点】】】】】】
     */
    @PostMapping("/backfill-exif")
    public String handleBackfillExif(RedirectAttributes redirectAttributes) {
        try {
            // 1. 调用 Service 方法
            String summary = photoService.backfillTakenAtExifData();

            // 2. 将成功摘要(Summary)发送回前端
            redirectAttributes.addFlashAttribute("success", summary);

        } catch (Exception e) {

            // 【【【【【【这是我们新增的调试代码】】】】】】
            // 强制在控制台打印完整的错误堆栈
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("!!! 捕获到“回填”功能的致命错误 !!!");
            e.printStackTrace(); // <--- 这一行将打印真正的错误
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            // 【【【【【【调试代码结束】】】】】】

            redirectAttributes.addFlashAttribute("error", "回填失败: " + e.getMessage());
        }

        // 3. 重定向回用户管理面板
        return "redirect:/admin/users";
    }

    /**
     * 【新增】获取最后一次维护日志的 API
     */
    @GetMapping("/maintenance-log")
    @ResponseBody
    public String getLastMaintenanceLog() {
        return photoService.getLastMaintenanceLog();
    }

    @GetMapping("/access-logs")
    @ResponseBody
    public List<AccessLog> getAccessLogs() {
        return accessLogRepository.findByUsernameNotOrderByVisitTimeDesc("Guest");
    }

    @GetMapping("/radar")
    public String showRadarPage() {
        return "radar";
    }
}