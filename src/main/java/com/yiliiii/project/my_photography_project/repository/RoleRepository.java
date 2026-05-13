package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.Role;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    // 允许我们通过 "ROLE_USER" 或 "ROLE_ADMIN" 来查找角色
    Optional<Role> findByName(String name);
}