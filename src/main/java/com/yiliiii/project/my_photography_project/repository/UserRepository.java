package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.User;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 【关键方法】
     * Spring Security 将调用这个方法来加载用户
     * (SELECT * FROM users WHERE username = ?)
     */
    Optional<User> findByUsername(String username);
}