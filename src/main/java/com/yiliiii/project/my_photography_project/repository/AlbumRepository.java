package com.yiliiii.project.my_photography_project.repository;
import com.yiliiii.project.my_photography_project.entity.Album;
import com.yiliiii.project.my_photography_project.entity.User;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    // 查找某个用户的所有相册
    List<Album> findByUserOrderByCreatedAtDesc(User user);
}