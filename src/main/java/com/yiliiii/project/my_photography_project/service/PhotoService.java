package com.yiliiii.project.my_photography_project.service;

import com.yiliiii.project.my_photography_project.dto.AiGeneratedContentDto;
import com.yiliiii.project.my_photography_project.dto.BatchFileDto;
import com.yiliiii.project.my_photography_project.dto.PhotoDetailDto;
import com.yiliiii.project.my_photography_project.entity.Album;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.Tag;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;
import com.yiliiii.project.my_photography_project.repository.TagRepository;
import com.yiliiii.project.my_photography_project.repository.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@SuppressWarnings("null")
public class PhotoService {

    private static final Logger logger = LoggerFactory.getLogger(PhotoService.class);

    // 【配置】全站统一的目标压缩质量 (0.90 = 90% 质量)
    private static final float TARGET_QUALITY = 0.90f;

    // 【新增配置】缩略图的长边像素限制 (建议 600 ~ 1000)
    // 改成 800 可以让图片在 Retina 屏上更清晰
    private static final int THUMB_SIZE = 800;

    // 【新增】暂存最后一次维护的日志
    private String lastMaintenanceLog = "暂无维护记录。请先执行一次维护任务。";

    public String getLastMaintenanceLog() {
        return lastMaintenanceLog;
    }

    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AiService aiService;
    @Autowired
    private ColorPaletteService colorPaletteService;

    // 注入新服务
    @Autowired
    private MetadataService metadataService;
    @Autowired
    private ImageProcessingService imageProcessingService;

    @Value("${myapp.upload-dir}")
    private String uploadDirString;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        try {
            this.uploadDir = Paths.get(this.uploadDirString);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDirString, e);
        }
    }

    /**
     * 数据清洗：将空字符串或纯空格转为 null
     */
    private String cleanString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null; // 强制转为 null
        }
        return input.trim(); // 去除首尾空格
    }

    // --- 业务方法 ---

    /**
     * 批量导入 (异步)
     */
    @Async
    public void batchImportPhotos(List<BatchFileDto> fileDtos, boolean generateAi, String username) {
        logger.info(">>> 批量导入服务已启动 (总数: " + fileDtos.size() + ", AI: " + generateAi + ")");

        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("无效的用户名: " + username));

        int successCount = 0;
        for (BatchFileDto fileDto : fileDtos) {
            String originalFilename = fileDto.getOriginalFilename();
            try {
                processSingleFile(fileDto, generateAi, uploader, originalFilename);
                successCount++;
            } catch (Exception e) {
                logger.error("!!! 批量导入失败 (文件: " + originalFilename + ")", e);
            }
        }
        logger.info(">>> 批量导入服务已完成 (成功: " + successCount + "/" + fileDtos.size() + ")");
    }

    /**
     * 处理单张文件 (用于批量导入)
     */
    @Transactional
    protected void processSingleFile(BatchFileDto fileDto, boolean generateAi, User uploader, String originalFilename)
            throws IOException, ExecutionException, InterruptedException {

        Photo photo = new Photo();
        byte[] fileBytes = fileDto.getContent();

        // 1. AI 生成
        if (generateAi) {
            CompletableFuture<AiGeneratedContentDto> aiFuture = aiService.generateTitleAndDescription(fileBytes);
            AiGeneratedContentDto aiContent = aiFuture.get();
            photo.setTitle(aiContent.getTitle());
            photo.setDescriptionLong(aiContent.getDescriptionLong());
            photo.setDescriptionShort(null);
        } else {
            photo.setTitle(originalFilename);
            photo.setDescriptionShort(null);
            photo.setDescriptionLong(null);
        }

        // 2. EXIF & GPS (Refactored)
        try (InputStream isForExif = new ByteArrayInputStream(fileBytes)) {
            metadataService.extractExifData(photo, isForExif);
        }

        // 【新增修复】如果 EXIF 没读到日期，使用当前时间兜底
        if (photo.getTakenAt() == null) {
            photo.setTakenAt(LocalDateTime.now());
        }

        // 3. 色板提取
        try {
            String palette = colorPaletteService.extractDominantColors(fileBytes);
            photo.setColorPalette(palette);
        } catch (Exception e) {
            logger.warn("色板提取失败", e);
        }

        // 4. 文件保存 (一图三存)
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String baseName = UUID.randomUUID().toString();

        String savedOriginalFilename = baseName + extension;
        String savedThumbnailFilename = baseName + "_thumb.jpg";
        String savedWebFilename = baseName + "_web.jpg";

        Path originalPath = this.uploadDir.resolve(savedOriginalFilename);
        Path thumbnailPath = this.uploadDir.resolve(savedThumbnailFilename);
        Path webPath = this.uploadDir.resolve(savedWebFilename);

        // A. 保存原图 (Refactored)
        try (InputStream isForSave = new ByteArrayInputStream(fileBytes)) {
            imageProcessingService.saveOriginal(isForSave, originalPath);
            photo.setImageUrl("/uploads/" + savedOriginalFilename);
        }
        // B. 保存缩略图 (Refactored)
        try (InputStream isForThumb = new ByteArrayInputStream(fileBytes)) {
            imageProcessingService.resizeAndSaveSquare(isForThumb, thumbnailPath, THUMB_SIZE, TARGET_QUALITY);
            photo.setThumbnailUrl("/uploads/" + savedThumbnailFilename);
        }
        // C. 保存Web大图 (Refactored)
        try (InputStream isForWeb = new ByteArrayInputStream(fileBytes)) {
            imageProcessingService.resizeAndSaveByWidth(isForWeb, webPath, 1920, TARGET_QUALITY);
            photo.setWebImageUrl("/uploads/" + savedWebFilename);
        }

        // 记录当前压缩质量，方便后续维护
        photo.setCompressionQuality(TARGET_QUALITY);

        photo.setUploader(uploader);
        photoRepository.save(photo);
    }

    /**
     * 保存单张新照片 (Controller调用)
     */
    @Transactional
    public void saveNewPhoto(Photo photo, MultipartFile file, String tagsString, String username) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("无法保存照片：必须上传一个有效的文件。");
        }

        try {
            User uploader = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("无效的用户名: " + username));

            byte[] fileBytes = file.getBytes();

            // EXIF (Refactored)
            try (InputStream isForExif = new ByteArrayInputStream(fileBytes)) {
                metadataService.extractExifData(photo, isForExif);
            }

            // 【新增修复】如果 EXIF 没读到日期，使用当前时间兜底
            if (photo.getTakenAt() == null) {
                photo.setTakenAt(LocalDateTime.now());
            }

            // Palette
            try {
                String palette = colorPaletteService.extractDominantColors(fileBytes);
                photo.setColorPalette(palette);
            } catch (Exception e) {
                logger.warn("色板提取失败", e);
            }

            // File Saving
            String extension = "";
            String originalFilenameStr = file.getOriginalFilename();
            if (originalFilenameStr != null && originalFilenameStr.lastIndexOf('.') > 0) {
                extension = originalFilenameStr.substring(originalFilenameStr.lastIndexOf('.'));
            }
            String baseName = UUID.randomUUID().toString();

            String originalFilename = baseName + extension;
            String thumbnailFilename = baseName + "_thumb.jpg";
            String webFilename = baseName + "_web.jpg";

            Path originalPath = this.uploadDir.resolve(originalFilename);
            Path thumbnailPath = this.uploadDir.resolve(thumbnailFilename);
            Path webPath = this.uploadDir.resolve(webFilename);

            // A. Original (Refactored)
            try (InputStream isForSave = new ByteArrayInputStream(fileBytes)) {
                imageProcessingService.saveOriginal(isForSave, originalPath);
                photo.setImageUrl("/uploads/" + originalFilename);
            }
            // B. Thumbnail (Refactored)
            try (InputStream isForThumb = new ByteArrayInputStream(fileBytes)) {
                imageProcessingService.resizeAndSaveSquare(isForThumb, thumbnailPath, THUMB_SIZE, TARGET_QUALITY);
                photo.setThumbnailUrl("/uploads/" + thumbnailFilename);
            }
            // C. Web Image (Refactored)
            try (InputStream isForWeb = new ByteArrayInputStream(fileBytes)) {
                imageProcessingService.resizeAndSaveByWidth(isForWeb, webPath, 1920, TARGET_QUALITY);
                photo.setWebImageUrl("/uploads/" + webFilename);
            }

            // Record Quality
            photo.setCompressionQuality(TARGET_QUALITY);

            // Tags & Meta
            Set<Tag> tags = processTags(tagsString);
            photo.setTags(tags);
            photo.setUploader(uploader);

            if (photo.getDescriptionLong() == null || photo.getDescriptionLong().isEmpty()) {
                photo.setDescriptionLong("（暂无详细简介）");
            }

            photoRepository.save(photo);

        } catch (Exception e) {
            throw new RuntimeException("保存新照片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新现有照片信息 (支持替换文件)
     * 修复：允许在不替换文件的情况下，手动修改拍摄时间
     */
    @Transactional
    public void updateExistingPhoto(Photo photo, MultipartFile file, String tagsString) {

        try {
            Photo oldPhoto = getPhotoById(photo.getId());

            String oldImageUrl = oldPhoto.getImageUrl();
            String oldThumbnailUrl = oldPhoto.getThumbnailUrl();
            String oldWebImageUrl = oldPhoto.getWebImageUrl();

            // 情况 A: 上传了新文件 -> 执行替换逻辑
            if (file != null && !file.isEmpty()) {
                byte[] fileBytes = file.getBytes();

                String extension = "";
                String originalFilenameStr = file.getOriginalFilename();
                if (originalFilenameStr != null && originalFilenameStr.lastIndexOf('.') > 0) {
                    extension = originalFilenameStr.substring(originalFilenameStr.lastIndexOf('.'));
                }
                String baseName = UUID.randomUUID().toString();

                String originalFilename = baseName + extension;
                String thumbnailFilename = baseName + "_thumb.jpg";
                String webFilename = baseName + "_web.jpg";

                Path originalPath = this.uploadDir.resolve(originalFilename);
                Path thumbnailPath = this.uploadDir.resolve(thumbnailFilename);
                Path webPath = this.uploadDir.resolve(webFilename);

                // 保存新文件 (Refactored)
                try (InputStream isForSave = new ByteArrayInputStream(fileBytes)) {
                    imageProcessingService.saveOriginal(isForSave, originalPath);
                    photo.setImageUrl("/uploads/" + originalFilename);
                }
                try (InputStream isForThumb = new ByteArrayInputStream(fileBytes)) {
                    imageProcessingService.resizeAndSaveSquare(isForThumb, thumbnailPath, THUMB_SIZE, TARGET_QUALITY);
                    photo.setThumbnailUrl("/uploads/" + thumbnailFilename);
                }
                try (InputStream isForWeb = new ByteArrayInputStream(fileBytes)) {
                    imageProcessingService.resizeAndSaveByWidth(isForWeb, webPath, 1920, TARGET_QUALITY);
                    photo.setWebImageUrl("/uploads/" + webFilename);
                }

                // 更新质量标记
                oldPhoto.setCompressionQuality(TARGET_QUALITY);

                // 提取新图的 EXIF (Refactored)
                try (InputStream isForExif = new ByteArrayInputStream(fileBytes)) {
                    metadataService.extractExifData(oldPhoto, isForExif);
                }
                // 如果新图里没日期，保留表单填的或者用当前时间
                if (oldPhoto.getTakenAt() == null) {
                    oldPhoto.setTakenAt(photo.getTakenAt() != null ? photo.getTakenAt() : LocalDateTime.now());
                }

                try {
                    String palette = colorPaletteService.extractDominantColors(fileBytes);
                    oldPhoto.setColorPalette(palette);
                } catch (Exception e) {
                    logger.warn("色板提取失败", e);
                }

                // 删除旧文件
                if (oldImageUrl != null)
                    deleteFile(oldImageUrl);
                if (oldThumbnailUrl != null)
                    deleteFile(oldThumbnailUrl);
                if (oldWebImageUrl != null)
                    deleteFile(oldWebImageUrl);

                // 更新 URL
                oldPhoto.setImageUrl(photo.getImageUrl());
                oldPhoto.setThumbnailUrl(photo.getThumbnailUrl());
                oldPhoto.setWebImageUrl(photo.getWebImageUrl());

            } else {
                // 情况 B: 没换文件 -> 更新信息
                // 【核心修改】信任表单提交的 EXIF 数据
                // 如果前端留空，这里 set 进去的就是空字符串或 null，相当于删除了信息

                // 【核心修复】使用 cleanString 清洗数据
                // 这样前端传来的 "" 会变成 null，存入数据库也是 null
                oldPhoto.setCameraModel(cleanString(photo.getCameraModel()));
                oldPhoto.setCameraMake(cleanString(photo.getCameraMake()));
                oldPhoto.setAperture(cleanString(photo.getAperture()));
                oldPhoto.setIso(cleanString(photo.getIso()));
                oldPhoto.setShutterSpeed(cleanString(photo.getShutterSpeed()));
                oldPhoto.setFocalLength(cleanString(photo.getFocalLength()));

                // 时间
                oldPhoto.setTakenAt(photo.getTakenAt());

                // 经纬度和色板
                oldPhoto.setLatitude(oldPhoto.getLatitude());
                oldPhoto.setLongitude(oldPhoto.getLongitude());
                oldPhoto.setColorPalette(oldPhoto.getColorPalette());
            }

            // 更新通用信息
            oldPhoto.getTags().clear();
            Set<Tag> tags = processTags(tagsString);

            oldPhoto.setTitle(photo.getTitle());
            oldPhoto.setDescriptionShort(photo.getDescriptionShort());
            oldPhoto.setDescriptionLong(photo.getDescriptionLong());
            oldPhoto.setTags(tags);

            photoRepository.save(oldPhoto);

        } catch (Exception e) {
            throw new RuntimeException("更新照片失败，文件IO错误: " + e.getMessage(), e);
        }
    }

    /**
     * 【智能增量版】全量维护工具
     * 逻辑：仅在 "文件丢失" 或 "当前画质 != 目标画质" 时才重新生成
     */
    @Transactional
    public String backfillTakenAtExifData() {
        logger.info(">>> [MAINTENANCE] 开始智能维护 (目标画质: " + TARGET_QUALITY + ")...");
        List<Photo> photosToUpdate = photoRepository.findAll();

        if (photosToUpdate.isEmpty())
            return "数据库中没有照片。";

        StringBuilder report = new StringBuilder();
        report.append(String.format("扫描 %d 张照片 | 目标画质: %.2f\n", photosToUpdate.size(), TARGET_QUALITY));
        report.append("--------------------------------------------------\n");

        int updatedCount = 0;
        int skippedCount = 0;
        int deletedCount = 0;

        for (Photo photo : photosToUpdate) {
            String imageUrl = photo.getImageUrl();
            List<String> fixActions = new java.util.ArrayList<>();
            boolean needsSave = false;

            // 1. 基础检查 (路径/原图是否存在)
            if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
                photoRepository.delete(photo);
                deletedCount++;
                report.append(String.format("❌ [删除] ID:%d | 无效路径\n", photo.getId()));
                continue;
            }
            String filename = imageUrl.substring("/uploads/".length());
            Path filePath = this.uploadDir.resolve(filename);
            if (!Files.exists(filePath)) {
                for (User user : photo.getUsersWhoLiked())
                    user.getLikedPhotos().remove(photo);
                photo.getUsersWhoLiked().clear();
                photoRepository.delete(photo);
                deletedCount++;
                report.append(String.format("❌ [删除] ID:%d | 原图丢失\n", photo.getId()));
                continue;
            }

            // 2. 智能判断：是否需要重制图片？
            // 条件 A: 数据库里没有记录质量 (说明是老图)
            // 条件 B: 记录的质量与现在设定的 TARGET_QUALITY 不一致 (说明标准变了)
            boolean qualityMismatch = (photo.getCompressionQuality() == null) ||
                    (Math.abs(photo.getCompressionQuality() - TARGET_QUALITY) > 0.001);

            // 3. 检查物理文件是否存在
            String thumbUrl = photo.getThumbnailUrl();
            String webUrl = photo.getWebImageUrl();
            boolean thumbMissing = thumbUrl == null
                    || !Files.exists(this.uploadDir.resolve(thumbUrl.substring("/uploads/".length())));
            boolean webMissing = webUrl == null
                    || !Files.exists(this.uploadDir.resolve(webUrl.substring("/uploads/".length())));

            // 4. 执行图片生成 (如果质量不达标 OR 文件缺失)
            if (qualityMismatch || thumbMissing || webMissing) {
                try {
                    String baseName = filename.substring(0, filename.lastIndexOf('.'));

                    // --- 重制缩略图 (Refactored) ---
                    String thumbName = baseName + "_thumb.jpg";
                    Path thumbPath = this.uploadDir.resolve(thumbName);
                    try (InputStream is = Files.newInputStream(filePath)) {
                        imageProcessingService.resizeAndSaveSquare(is, thumbPath, THUMB_SIZE, TARGET_QUALITY);
                        photo.setThumbnailUrl("/uploads/" + thumbName);
                    }

                    // --- 重制 Web 图 (Refactored) ---
                    String webName = baseName + "_web.jpg";
                    Path webPath = this.uploadDir.resolve(webName);
                    try (InputStream is = Files.newInputStream(filePath)) {
                        imageProcessingService.resizeAndSaveByWidth(is, webPath, 1920, TARGET_QUALITY);
                        photo.setWebImageUrl("/uploads/" + webName);
                    }

                    // 更新数据库标记
                    photo.setCompressionQuality(TARGET_QUALITY);

                    String reason = qualityMismatch ? "画质升级" : "文件补全";
                    fixActions.add(reason + " (->" + TARGET_QUALITY + ")");
                    needsSave = true;

                } catch (Exception e) {
                    logger.error("图片生成失败 ID: " + photo.getId(), e);
                    report.append(String.format("⚠️ [失败] ID:%d | %s\n", photo.getId(), e.getMessage()));
                }
            }

            // 5. 修复 EXIF (如果缺失)
            if (photo.getTakenAt() == null) {
                try (InputStream is = Files.newInputStream(filePath)) {
                    metadataService.extractExifData(photo, is); // Refactored
                    if (photo.getTakenAt() != null) {
                        fixActions.add("修复EXIF");
                        needsSave = true;
                    }
                } catch (Exception e) {
                    /* 忽略 */ }

                // 还没修好？用文件时间兜底
                if (photo.getTakenAt() == null) {
                    try {
                        java.nio.file.attribute.FileTime ft = Files.getLastModifiedTime(filePath);
                        photo.setTakenAt(LocalDateTime.ofInstant(ft.toInstant(), java.time.ZoneId.systemDefault()));
                        fixActions.add("回填文件时间");
                        needsSave = true;
                    } catch (Exception e) {
                        photo.setTakenAt(LocalDateTime.now());
                        fixActions.add("回填当前时间");
                        needsSave = true;
                    }
                }
            }

            // 6. 修复色板 (如果缺失)
            if (photo.getColorPalette() == null) {
                try (InputStream is2 = Files.newInputStream(filePath)) {
                    String newPalette = colorPaletteService.extractDominantColors(is2.readAllBytes());
                    photo.setColorPalette(newPalette);
                    fixActions.add("生成色板");
                    needsSave = true;
                } catch (Exception e) {
                    /* 忽略 */ }
            }

            if (needsSave) {
                photoRepository.save(photo);
                updatedCount++;
                report.append(String.format("✅ [更新] ID:%d | %s\n", photo.getId(), String.join(", ", fixActions)));
            } else {
                skippedCount++;
            }
        }

        report.append("--------------------------------------------------\n");
        report.append(
                String.format("维护总结: 更新 %d 张, 跳过 %d 张 (无需更新), 删除 %d 张.", updatedCount, skippedCount, deletedCount));

        // 【新增】保存到内存变量
        this.lastMaintenanceLog = report.toString();
        return report.toString();
    }

    /**
     * 删除照片
     * 修复：增加“解除相册关联”逻辑，防止 500 外键错误
     */
    @Transactional
    public void deletePhotoById(@Nonnull Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("无效的照片ID: " + id));

        // 1. 解除“用户点赞”关联
        for (User user : photo.getUsersWhoLiked()) {
            user.getLikedPhotos().remove(photo);
        }
        photo.getUsersWhoLiked().clear();

        // 2. 【核心修复】解除“相册”关联
        // 如果不先从相册里把它拿出来，数据库会报错 (Foreign Key Constraint)
        // 我们创建一个副本列表进行遍历，防止 ConcurrentModificationException
        if (photo.getAlbums() != null) {
            for (Album album : new java.util.ArrayList<>(photo.getAlbums())) {
                album.getPhotos().remove(photo);
                // 在 @Transactional 事务中，JPA 会自动监测到 album 的变化并更新中间表
            }
            photo.getAlbums().clear();
        }

        // 3. 删除数据库记录
        // (注：Photo 实体里的 comments 配置了 CascadeType.ALL，所以评论会自动删，不用管)
        // (注：Photo 实体拥有的 tags 关联会自动在中间表清理，不用管)
        photoRepository.delete(photo);
        photoRepository.flush(); // 立即提交，确保数据库没报错再删文件

        // 4. 删除物理文件
        try {
            if (photo.getImageUrl() != null)
                deleteFile(photo.getImageUrl());
            if (photo.getThumbnailUrl() != null)
                deleteFile(photo.getThumbnailUrl());
            if (photo.getWebImageUrl() != null)
                deleteFile(photo.getWebImageUrl());
        } catch (IOException e) {
            logger.error("删除照片物理文件时出错 (ID: {})：{}", id, e.getMessage());
            // 文件删不掉不影响业务，只记录日志
        }
    }

    public Page<Photo> getAllPhotos(String query, @Nonnull Pageable pageable) {
        if (query != null && !query.trim().isEmpty()) {
            String trimmedQuery = query.trim();
            if (trimmedQuery.matches("^[0-9a-fA-F]{6}$")) {
                String colorCode = "#" + trimmedQuery.toUpperCase();
                return photoRepository.findByColorPaletteContaining(colorCode, pageable);
            }
            return photoRepository.findByTitleContainingIgnoreCaseOrDescriptionShortContainingIgnoreCase(trimmedQuery,
                    trimmedQuery, pageable);
        } else {
            return photoRepository.findAll(pageable);
        }
    }

    public List<Photo> getAllPhotosForMap() {
        return photoRepository.findAll();
    }

    public Photo getPhotoById(@Nonnull Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("无效的照片ID:" + id));
    }

    @Transactional(readOnly = true)
    public PhotoDetailDto getPhotoDetails(@Nonnull Long photoId) {
        Photo photo = photoRepository.findByIdWithDetails(photoId)
                .orElseThrow(() -> new IllegalArgumentException("无效的照片ID:" + photoId));
        return new PhotoDetailDto(photo);
    }

    @Transactional
    public int toggleLike(@Nonnull Long photoId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("无效的用户名:" + username));
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("无效的照片ID:" + photoId));

        int currentCount = photo.getLikeCount();
        if (user.getLikedPhotos().contains(photo)) {
            user.getLikedPhotos().remove(photo);
            return currentCount - 1;
        } else {
            user.getLikedPhotos().add(photo);
            User owner = photo.getUploader();
            if (owner != null) {
                notificationService.createAndSend(owner, user, photo, "LIKE", null);
            }
            return currentCount + 1;
        }
    }

    // --- 内部辅助方法 ---

    private Set<Tag> processTags(String tagsString) {
        Set<Tag> tags = new HashSet<>();
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return tags;
        }
        String[] tagNames = tagsString.split(",");
        for (String name : tagNames) {
            String trimmedName = name.trim();
            if (trimmedName.isEmpty())
                continue;
            Tag tag = tagRepository.findByName(trimmedName)
                    .orElseGet(() -> tagRepository.save(new Tag(trimmedName)));
            tags.add(tag);
        }
        return tags;
    }

    private void deleteFile(String imageUrl) throws IOException {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        String filename = imageUrl.substring("/uploads/".length());
        Path filePath = this.uploadDir.resolve(filename).toAbsolutePath();
        Files.deleteIfExists(filePath);
    }
}