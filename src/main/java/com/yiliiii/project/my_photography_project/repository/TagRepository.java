package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.Tag;
import com.yiliiii.project.my_photography_project.service.PhotoService;
 // 1. 你的包名

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // 导入 Optional, 用于更安全地处理 null

/**
 * Tag Repository (仓库) 接口
 * * 继承 JpaRepository<Tag, Long> 意味着:
 * 1. Tag: 这个 Repository 是用来管理 "Tag" 实体的。
 * 2. Long: "Tag" 实体的主键 (id) 类型是 "Long"。
 */
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * 【自定义查询】
     * 这是 Spring Data JPA 的“魔法”之一。
     * 你只需要按照 "find...By..." 的格式定义方法名,
     * Spring Boot 就会在运行时自动为你实现这个查询。
     *
     * "findByName" -> "SELECT * FROM tag WHERE name = ?"
     *
     * 我们用 Optional<Tag> 是为了更安全地处理 "可能不存在" 的情况。
     * 这被 PhotoService 用来检查一个标签是否已经存在于数据库中。
     *
     * @param name 要查找的标签名
     * @return 一个包含 Tag (如果找到) 或为空 (如果没找到) 的 Optional
     */
    Optional<Tag> findByName(String name);
}