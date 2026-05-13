package com.yiliiii.project.my_photography_project.service;

import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;
import com.yiliiii.project.my_photography_project.repository.TagRepository;
import com.yiliiii.project.my_photography_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AiService aiService;
    @Mock
    private ColorPaletteService colorPaletteService;
    @Mock
    private MetadataService metadataService;
    @Mock
    private ImageProcessingService imageProcessingService;

    @InjectMocks
    private PhotoService photoService;

    @BeforeEach
    void setup() {
        // Initialize the Path field directly since @PostConstruct doesn't run
        ReflectionTestUtils.setField(photoService, "uploadDir", Paths.get("test-uploads"));
    }

    @Test
    void testDeletePhotoById() {
        Long photoId = 123L;
        Photo photo = new Photo();
        photo.setId(photoId);
        photo.setImageUrl("/uploads/test.jpg");
        photo.setUsersWhoLiked(new HashSet<>());
        photo.setAlbums(new HashSet<>());
        photo.setTags(new HashSet<>());

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        photoService.deletePhotoById(photoId);

        verify(photoRepository).delete(photo);
    }

    @Test
    void testToggleLike() {
        Long photoId = 1L;
        String username = "user1";

        User user = new User();
        user.setUsername(username);
        user.setLikedPhotos(new HashSet<>());

        Photo photo = new Photo();
        photo.setId(photoId);
        photo.setUsersWhoLiked(new HashSet<>());

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        // 1. First like
        photoService.toggleLike(photoId, username);

        assert (user.getLikedPhotos().contains(photo));

        // 2. Toggle again (unlike)
        photoService.toggleLike(photoId, username);
        assert (!user.getLikedPhotos().contains(photo));
    }
}
