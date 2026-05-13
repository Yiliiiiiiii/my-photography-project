package com.yiliiii.project.my_photography_project.controller;

import com.yiliiii.project.my_photography_project.dto.PhotoGroupDto;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.entity.User;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;
import com.yiliiii.project.my_photography_project.repository.UserRepository;
import com.yiliiii.project.my_photography_project.service.DailySummaryService;
import com.yiliiii.project.my_photography_project.service.PhotoService;
// 1. 你的包名

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

// Spring Security 相关
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

// Java 时间与工具类
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// 【新增导入】用于照片墙
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageImpl;
import java.util.function.Function;

@Controller
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private DailySummaryService dailySummaryService;

    /**
     * 落地页 (Landing Page)
     */
    @GetMapping("/")
    public String showLandingPage(Model model) {
        long photoCount = photoRepository.count();
        long userCount = userRepository.count();

        model.addAttribute("totalPhotos", photoCount);
        model.addAttribute("totalUsers", userCount);

        return "landing";
    }

    /**
     * 显示地图页面
     */
    @GetMapping("/map")
    public String showMapPage() {
        return "map";
    }

    /**
     * 画廊首页 (Gallery)
     */
    @GetMapping("/gallery")
    public String viewHomePage(Model model,
            @PageableDefault(size = 9) Pageable pageable,
            @RequestParam(value = "query", required = false) String query) {

        // 1. 构造排序规则
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "takenAt").and(Sort.by(Sort.Direction.DESC, "id")));

        // 2. 计算上一页最后一天的日期
        LocalDate lastDateFromPreviousPage = null;
        if (query == null && pageable.getPageNumber() > 0) {
            try {
                Pageable previousPageable = sortedPageable.previousOrFirst();
                Page<Photo> previousPage = photoService.getAllPhotos(null, previousPageable);

                if (!previousPage.isEmpty()) {
                    List<Photo> prevContent = previousPage.getContent();
                    Photo lastPhotoOfPrevPage = prevContent.get(prevContent.size() - 1);
                    if (lastPhotoOfPrevPage.getTakenAt() != null) {
                        lastDateFromPreviousPage = lastPhotoOfPrevPage.getTakenAt().toLocalDate();
                    }
                }
            } catch (Exception e) {
                /* 忽略 */ }
        }

        // 3. 获取当前页数据
        Page<Photo> photoPage = photoService.getAllPhotos(query, sortedPageable);

        // 4. 分组
        Map<LocalDate, List<Photo>> photosByDate = groupPhotosByDate(photoPage.getContent());

        // 5. 获取每日总结
        List<LocalDate> dates = photosByDate.keySet().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<LocalDate, String> summaries = dailySummaryService.getSummariesForDates(dates);

        // 6. 构建前端 DTO
        List<PhotoGroupDto> photoGroups = createPhotoGroupDtos(
                photosByDate,
                summaries,
                lastDateFromPreviousPage);

        model.addAttribute("photoGroups", photoGroups);
        model.addAttribute("photoPage", photoPage);
        model.addAttribute("query", query);

        // 7. 传递点赞状态
        addLikedPhotosToModel(model);

        return "index";
    }

    @GetMapping("/gallery/3d")
    public String view3DGallery() {
        return "gallery-3d";
    }

    /**
     * 【新增】沉浸式单图浏览 (Vision Pro Style)
     */
    @GetMapping("/immersive/{id}")
    public String showImmersivePage(@PathVariable("id") Long id, Model model) {
        model.addAttribute("photo", photoService.getPhotoById(id));
        return "immersive";
    }

    /**
     * --- 【关键修复】照片墙页面 (完美随机不重复版) ---
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/wall")
    public String showPhotoWall(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpSession session) {

        // 1. 获取或初始化随机 ID 列表
        List<Long> shuffledIds = (List<Long>) session.getAttribute("WALL_SHUFFLED_IDS");

        // 如果是第 0 页（代表用户刚刷新页面），或者 Session 里没数据，就重新生成一次随机列表
        if (page == 0 || shuffledIds == null) {
            shuffledIds = photoRepository.findAllIds(); // 只查 ID，速度极快
            Collections.shuffle(shuffledIds); // 在内存中洗牌
            session.setAttribute("WALL_SHUFFLED_IDS", shuffledIds); // 存入会话
        }

        // 2. 手动计算分页切片
        int total = shuffledIds.size();
        int start = page * size;
        int end = Math.min(start + size, total);

        List<Photo> pageContent = new ArrayList<>();

        if (start < end) {
            // 取出当前页需要的 ID 列表
            List<Long> pageIds = shuffledIds.subList(start, end);

            // 去数据库查这些 ID 对应的详细数据
            @SuppressWarnings("null")
            List<Photo> photos = photoRepository.findAllById(pageIds);

            // 【关键】findAllById 返回的顺序不一定是 pageIds 的顺序
            // 所以我们需要在内存中重新排序
            Map<Long, Photo> photoMap = photos.stream()
                    .collect(Collectors.toMap(Photo::getId, Function.identity()));

            for (Long id : pageIds) {
                if (photoMap.containsKey(id)) {
                    pageContent.add(photoMap.get(id));
                }
            }
        }

        // 3. 包装成 Page 对象欺骗前端
        Page<Photo> photoPage = new PageImpl<>(pageContent, PageRequest.of(page, size), total);

        model.addAttribute("photoPage", photoPage);

        // 4. 传递点赞状态
        addLikedPhotosToModel(model);

        return "wall";
    }

    @GetMapping("/add")
    public String showAddPhotoForm(Model model) {
        model.addAttribute("photo", new Photo());
        return "add-photo";
    }

    @PostMapping("/save")
    public String savePhoto(Photo photo,
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "tagsString", required = false, defaultValue = "") String tagsString,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        photoService.saveNewPhoto(photo, file, tagsString, auth.getName());
        return "redirect:/gallery";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("photo", photoService.getPhotoById(id));
        return "edit-photo";
    }

    @PostMapping("/update")
    public String updatePhoto(Photo photo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "tagsString", required = false, defaultValue = "") String tagsString) {

        photoService.updateExistingPhoto(photo, file, tagsString);
        // 定位到刚编辑的那张图
        return "redirect:/gallery#photo-" + photo.getId();
    }

    @PostMapping("/delete/{id}")
    public String deletePhoto(@PathVariable("id") Long id) {
        photoService.deletePhotoById(id);
        return "redirect:/gallery";
    }

    // --- 辅助方法 ---

    private Map<LocalDate, List<Photo>> groupPhotosByDate(List<Photo> photos) {
        Map<LocalDate, List<Photo>> grouped = new LinkedHashMap<>();
        if (photos == null || photos.isEmpty()) {
            return grouped;
        }

        for (Photo photo : photos) {
            LocalDate photoDate = null;
            if (photo.getTakenAt() != null) {
                photoDate = photo.getTakenAt().toLocalDate();
            }
            grouped.computeIfAbsent(photoDate, k -> new ArrayList<>()).add(photo);
        }
        return grouped;
    }

    private List<PhotoGroupDto> createPhotoGroupDtos(
            Map<LocalDate, List<Photo>> photosByDate,
            Map<LocalDate, String> summaries,
            LocalDate lastDateFromPreviousPage) {
        List<PhotoGroupDto> dtoList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE);
        boolean isFirstGroup = true;

        for (Map.Entry<LocalDate, List<Photo>> entry : photosByDate.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<Photo> photos = entry.getValue();
            String summary = summaries.get(currentDate);

            String displayDate;
            boolean merge = false;

            if (currentDate == null) {
                displayDate = "拍摄时间未知";
            } else {
                displayDate = currentDate.format(formatter);
                if (isFirstGroup && lastDateFromPreviousPage != null && currentDate.equals(lastDateFromPreviousPage)) {
                    merge = true;
                }
            }

            dtoList.add(new PhotoGroupDto(displayDate, currentDate, photos, summary, merge));
            isFirstGroup = false;
        }
        return dtoList;
    }

    // 提取公共的点赞状态获取逻辑
    private void addLikedPhotosToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<Long> likedPhotoIds = Collections.emptySet();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            User currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null) {
                likedPhotoIds = currentUser.getLikedPhotos().stream()
                        .map(Photo::getId)
                        .collect(Collectors.toSet());
            }
        }
        model.addAttribute("likedPhotoIds", likedPhotoIds);
    }
}