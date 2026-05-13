package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.Comment;
import com.yiliiii.project.my_photography_project.entity.Photo;
 // 1. 你的包名

import org.springframework.data.jpa.repository.JpaRepository;
// 导入 List 和 Comment

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // (我们暂时不需要自定义查询, 
    //  因为 "保存" JpaRepository 已经提供了,
    //  "查找" 将通过 Photo.getComments() 实现)
}