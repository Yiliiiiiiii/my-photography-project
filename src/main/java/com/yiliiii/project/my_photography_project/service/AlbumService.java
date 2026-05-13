package com.yiliiii.project.my_photography_project.service;
import com.yiliiii.project.my_photography_project.entity.Album;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.AlbumRepository;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;
import com.yiliiii.project.my_photography_project.repository.UserRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@SuppressWarnings("null")
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PhotoRepository photoRepository;

    public List<Album> getUserAlbums(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return albumRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Album getAlbumById(Long id) {
        return albumRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("相册不存在"));
    }

    @Transactional
    public Album createAlbum(String title, String description, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Album album = new Album();
        album.setTitle(title);
        album.setDescription(description);
        album.setUser(user);
        // 默认封面
        album.setCoverImageUrl("/images/default_album_cover.png"); // 你需要准备一张默认图，或者留空
        return albumRepository.save(album);
    }

    @Transactional
    public void addPhotoToAlbum(Long albumId, Long photoId, String username) {
        Album album = getAlbumById(albumId);
        // 权限检查：只有相册主人才可以加照片
        if (!album.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("无权操作此相册");
        }

        Photo photo = photoRepository.findById(photoId).orElseThrow();

        // 如果相册是空的，把第一张图设为封面
        if (album.getPhotos().isEmpty()) {
            // 优先用缩略图
            String cover = photo.getThumbnailUrl() != null ? photo.getThumbnailUrl() : photo.getImageUrl();
            album.setCoverImageUrl(cover);
        }

        album.getPhotos().add(photo);
        albumRepository.save(album);
    }

    @Transactional
    public void removePhotoFromAlbum(Long albumId, Long photoId, String username) {
        Album album = getAlbumById(albumId);
        if (!album.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("无权操作");
        }
        Photo photo = photoRepository.findById(photoId).orElseThrow();
        album.getPhotos().remove(photo);
        albumRepository.save(album);
    }

    @Transactional
    public void deleteAlbum(Long albumId, String username) {
        Album album = getAlbumById(albumId);
        if (!album.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("无权操作");
        }
        albumRepository.delete(album);
    }
}