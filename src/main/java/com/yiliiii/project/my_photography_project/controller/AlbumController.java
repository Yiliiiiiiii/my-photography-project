package com.yiliiii.project.my_photography_project.controller;
import com.yiliiii.project.my_photography_project.entity.Album;
import com.yiliiii.project.my_photography_project.service.AlbumService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    // 1. 相册列表页
    @GetMapping("/albums")
    public String showAlbums(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";
        List<Album> albums = albumService.getUserAlbums(auth.getName());
        model.addAttribute("albums", albums);
        return "albums"; // templates/albums.html
    }

    // 2. 相册详情页 (查看里面的照片)
    @GetMapping("/album/{id}")
    public String showAlbumDetail(@PathVariable Long id, Model model) {
        Album album = albumService.getAlbumById(id);
        model.addAttribute("album", album);
        return "album-view"; // templates/album-view.html
    }

    // 3. API: 创建相册
    @PostMapping("/api/album/create")
    @ResponseBody
    public ResponseEntity<?> createAlbum(@RequestParam("title") String title, 
                                         @RequestParam("description") String description,
                                         Authentication auth) {
        if (auth == null) return ResponseEntity.status(403).build();
        Album album = albumService.createAlbum(title, description, auth.getName());
        return ResponseEntity.ok(Map.of("id", album.getId(), "title", album.getTitle()));
    }

    // 4. API: 把照片加入相册
    @PostMapping("/api/album/{albumId}/add/{photoId}")
    @ResponseBody
    public ResponseEntity<?> addPhoto(@PathVariable Long albumId, @PathVariable Long photoId, Authentication auth) {
        try {
            albumService.addPhotoToAlbum(albumId, photoId, auth.getName());
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 5. API: 获取我的所有相册 (用于弹窗里的下拉菜单)
    @GetMapping("/api/my-albums")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMyAlbums(Authentication auth) {
        if (auth == null) return ResponseEntity.status(403).build();
        List<Album> albums = albumService.getUserAlbums(auth.getName());
        List<Map<String, Object>> result = albums.stream().map(a -> Map.of(
            "id", (Object)a.getId(),
            "title", (Object)a.getTitle()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
    
    // 6. 删除相册
    @PostMapping("/album/delete/{id}")
    public String deleteAlbum(@PathVariable Long id, Authentication auth) {
        albumService.deleteAlbum(id, auth.getName());
        return "redirect:/albums";
    }
}