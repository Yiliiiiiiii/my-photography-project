package com.yiliiii.project.my_photography_project.service;
import com.yiliiii.project.my_photography_project.entity.Role;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.RoleRepository;
import com.yiliiii.project.my_photography_project.repository.UserRepository;


import java.io.IOException;
import java.io.InputStream;
// import java.nio.file.Files; // (Unused)
// import java.nio.file.Paths; // (Unused)
// import java.nio.file.StandardCopyOption; // (Unused)
import java.util.UUID;

import java.nio.file.Path;
import java.nio.file.Paths; // <-- 【【【修复】】】: 这个 Paths 是需要的
import java.nio.file.Files; // <-- 【【【修复】】】: 这个 Files 是需要的
import java.nio.file.StandardCopyOption; // <-- 【【【修复】】】: 这个也是需要的

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Nonnull; // <-- 【新增】
import java.util.Collections;
import java.util.HashSet; // <-- 【【【修复】】】: 这个是需要的

import java.util.List;
// import java.util.HashSet; // (Duplicate)
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${myapp.avatar-upload-dir}")
    private String avatarUploadDirString;

    private Path avatarUploadDir;

    @PostConstruct
    public void init() {
        try {
            avatarUploadDir = Paths.get(avatarUploadDirString);
            if (!Files.exists(avatarUploadDir)) {
                Files.createDirectories(avatarUploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建头像上传目录", e);

        }
    }

    /**
     * 注册新用户
     * 逻辑变更：如果是系统的第一个用户，自动赋予管理员权限；后续用户均为游客。
     */
    @Transactional
    public boolean registerNewVisitor(String username, String password) {

        // 1. 检查重名
        if (userRepository.findByUsername(username).isPresent()) {
            return false;
        }

        // 2. 创建用户实体
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAvatarUrl("/images/default_avatar.png");

        // 3. 【核心修改】判断角色
        // 如果数据库里没有任何用户 (count == 0)，这第一个人就是管理员
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: ROLE_ADMIN not found."));
            newUser.setRoles(Collections.singleton(adminRole));
            System.out.println(">>>  首位用户注册 [" + username + "]，已设置管理员！");
        } else {
            // 否则，只是普通游客
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Error: ROLE_USER not found."));
            newUser.setRoles(Collections.singleton(userRole));
        }

        // 4. 保存
        userRepository.save(newUser);
        return true;
    }

    /**
     * 更新用户头像的核心方法
     */
    public void updateUserAvatar(String username, MultipartFile file) throws IOException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("无效的用户"));

        String oldAvatarUrl = user.getAvatarUrl();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String newFilename = UUID.randomUUID().toString() + extension;
        Path targetPath = this.avatarUploadDir.resolve(newFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String newAvatarUrl = "/uploads/avatars/" + newFilename;
        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);

        if (oldAvatarUrl != null && !oldAvatarUrl.startsWith("/images/")) {
            deleteFileByUrl(oldAvatarUrl);
        }
    }

    /**
     * (管理面板) 获取所有用户
     */
    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * (管理面板) 获取所有可用的角色
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * (管理面板) 通过 ID 获取单个用户 (用于编辑页)
     */
    @SuppressWarnings("null")
    public User getUserById(@Nonnull Long id) { // <-- 【【【修复】】】: 添加 @Nonnull
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("无效的用户ID: " + id));
    }

    /**
     * (管理面板) 更新一个用户的角色
     */
    @Transactional
    public void updateUserRoles(@Nonnull Long userId, List<Long> roleIds) { // <-- 【【【修复】】】: 添加 @Nonnull
        User user = getUserById(userId); // <-- @Nonnull 已修复此处的警告

        if (roleIds == null || roleIds.isEmpty()) {
            user.getRoles().clear();
        } else {
            List<Role> roles = roleRepository.findAllById(roleIds);
            user.setRoles(new HashSet<>(roles)); // <-- HashSet 导入是需要的
        }

        userRepository.save(user);
    }

    /**
     * (管理面板) 删除一个用户
     */
    @Transactional
    public void deleteUser(@Nonnull Long userIdToDelete, String currentAdminUsername) { // <-- 【【【修复】】】: 添加 @Nonnull
        User userToDelete = getUserById(userIdToDelete); // <-- @Nonnull 已修复此处的警告

        if (userToDelete.getUsername().equals(currentAdminUsername)) {
            throw new IllegalArgumentException("操作失败：管理员不能删除自己的账户。");
        }

        userRepository.delete(userToDelete);
    }

    /**
     * 删除文件的辅助方法
     */
    private void deleteFileByUrl(String fileUrl) throws IOException {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/avatars/")) {
            return;
        }
        String filename = fileUrl.substring("/uploads/avatars/".length());
        Path filePath = this.avatarUploadDir.resolve(filename);
        Files.deleteIfExists(filePath);
    }

    /**
     * (个人资料) 更改用户名
     */
    @Transactional
    public boolean changeUsername(String currentUsername, String newUsername) {
        if (userRepository.findByUsername(newUsername).isPresent()) {
            return false;
        }
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("无效的当前用户"));
        user.setUsername(newUsername);
        userRepository.save(user);
        return true;
    }

    /**
     * (个人资料) 更改密码
     */
    @Transactional
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("无效的当前用户"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}