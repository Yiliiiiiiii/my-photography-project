package com.yiliiii.project.my_photography_project.service;
import com.yiliiii.project.my_photography_project.dto.AiGeneratedContentDto;
import com.yiliiii.project.my_photography_project.entity.Photo;
import com.yiliiii.project.my_photography_project.repository.PhotoRepository;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
// import java.util.Optional; // (Unused)
import net.coobird.thumbnailator.Thumbnails;
import java.io.ByteArrayOutputStream;

// (用于调试的导入)
// import com.fasterxml.jackson.databind.ObjectMapper; // (Unused)
// import reactor.core.publisher.Mono; // (Unused)
// import org.springframework.http.HttpStatusCode; // (Unused)

// 【【【新增导入】】】
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference; // <-- 【新增】 用于类型安全
import jakarta.annotation.Nonnull; // <-- 【新增】 用于 Null 安全

// 在 AiService.java 中
import org.springframework.scheduling.annotation.Async; // 【【【新增】】】
import java.util.concurrent.CompletableFuture; // 【【【新增】】】
import com.fasterxml.jackson.databind.ObjectMapper; // 【【【新增】】】

@Service
@SuppressWarnings("null")
public class AiService {

    private final WebClient webClient;
    private final PhotoRepository photoRepository;
    // private final ObjectMapper objectMapper; // (Unused)

    @Value("${alibaba.api.key}")
    private String apiKey;

    // 【【【新增】】】: 注入文件根路径
    @Value("${myapp.upload-dir}")
    private String uploadDirString;

    private Path uploadDir;
    private final ObjectMapper objectMapper;

    // 【【【修改】】】: 确保 ObjectMapper 已注入
    public AiService(PhotoRepository photoRepository,
            @Value("${myapp.upload-dir}") String uploadDirString,
            ObjectMapper objectMapper) { // <-- 注入
        this.photoRepository = photoRepository;
        this.uploadDir = Paths.get(uploadDirString);
        this.objectMapper = objectMapper; // <-- 赋值

        // ... (WebClient baseUrl 不变) ...
        this.webClient = WebClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }

    /**
     * (同步执行)
     */
    @Transactional // 确保数据库更新成功
    public String generateDescription(@Nonnull Long photoId) throws Exception { // <-- 【【【修复】】】: 添加 @Nonnull

        System.out.println("AI 同步任务 (Qwen-Compat) 开始... Photo ID: " + photoId);

        // 1. 查找照片并获取其*物理*文件路径
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("无效的照片ID"));

        // ... (读取文件的逻辑不变) ...
        String imageUrl = photo.getImageUrl();
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            throw new RuntimeException("照片 URL 无效或非本地文件");
        }
        String filename = imageUrl.substring("/uploads/".length());
        Path imagePath = this.uploadDir.resolve(filename);
        if (!Files.exists(imagePath)) {
            throw new RuntimeException("磁盘上找不到物理文件: " + imagePath);
        }
        byte[] imageBytes = Files.readAllBytes(imagePath);

        // 3. (不变) 压缩图片
        // ... (压缩逻辑不变) ...
        InputStream imageInputStream = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(imageInputStream)
                .size(1024, 1024)
                .outputFormat("jpeg")
                .toOutputStream(outputStream);
        byte[] resizedImageBytes = outputStream.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(resizedImageBytes);

        // 4. (不变) 构建请求体
        Map<String, Object> requestBody = buildOpenAiCompatibleRequestBody(base64Image);

        // 5. 【【【修改】】】: 调用 API
        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody) // <-- 'bodyValue' 的 Null 警告已通过 @Nonnull 修复
                .retrieve()
                // ... (onStatus 错误处理不变) ...

                // 【【【【【【修复】】】】】】: 使用 ParameterizedTypeReference 消除 Map 警告
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();

        // 6. (不变) 解析响应
        String aiDescription = parseOpenAiCompatibleResponse(response);

        // 7. 【【【修改】】】: 保存并返回
        photo.setDescriptionLong(aiDescription);
        photoRepository.save(photo);
        System.out.println("AI 任务 (Qwen-Compat) 同步完成！Photo ID: " + photoId);

        return aiDescription; // <-- 返回结果
    }

    /**
     * 【【【【【【新增：批量导入的 AI 核心】】】】】】
     * 此方法在后台线程运行, 接收原始图像字节,
     * 返回一个包含标题和简介的 DTO。
     */
    // 在 AiService.java 中

    @Async
    public CompletableFuture<AiGeneratedContentDto> generateTitleAndDescription(byte[] imageBytes) {

        System.out.println("AI 批量任务 (Qwen-Compat) 异步开始...");

        try {
            // 1. (不变) 压缩图片
            String base64Image = compressAndEncodeImage(imageBytes);

            // 2. (不变) 构建请求体 (使用我们更新后的“严格 JSON”提示词)
            Map<String, Object> requestBody = buildAiBatchRequestBody(base64Image);

            // 3. (不变) 调用 API
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            // 4. 解析 AI 的原始响应 (例如 "```json\n{...}\n```")
            String aiRawContent = parseOpenAiCompatibleResponse(response);

            // 5. 【【【修复点 1：变量名统一】】】
            // 调用“清洁工”来提取 {..} 块
            String aiCleanJson = extractJsonBlock(aiRawContent); // <-- 确保这里使用了第 4 步的变量

            // 6. 【【【修复点 2：消除重复】】】
            // *只*调用一次 objectMapper.readValue，使用 *干净* 的 JSON
            AiGeneratedContentDto resultDto = objectMapper.readValue(
                    aiCleanJson, // <-- 使用 aiCleanJson
                    AiGeneratedContentDto.class);

            System.out.println("AI 批量任务 (Qwen-Compat) 异步完成!");
            return CompletableFuture.completedFuture(resultDto);

        } catch (Exception e) {
            System.err.println("AI 批量任务失败: " + e.getMessage());
            // (不变) 返回“B计划” DTO
            return CompletableFuture
                    .completedFuture(new AiGeneratedContentDto("AI 生成失败", "AI 生成失败: " + e.getMessage()));
        }
    }

    /**
     * 【【【新增】】】: 压缩逻辑 (从旧方法中提取)
     */
    private String compressAndEncodeImage(byte[] imageBytes) throws Exception {
        try (InputStream imageInputStream = new ByteArrayInputStream(imageBytes);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnails.of(imageInputStream)
                    .size(1024, 1024)
                    .outputFormat("jpeg")
                    .toOutputStream(outputStream);
            byte[] resizedImageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(resizedImageBytes);
        }
    }

    /**
     * 【【【新增】】】: 批量 AI 的新提示词 (Prompt)
     */
    private Map<String, Object> buildAiBatchRequestBody(String base64Image) {
        String prompt = "你是一位摄影作品集策展人。" +
                "也是一个 JSON 格式化工具。" +
                "1. 为这张图起一个'标题' (title)。不要使用“某某的某某”这种格式。语言自然而有深意。使用四字或五字的标题。不允许出现“时光”的字样" +
                // 【【【【【【关键修复：'description' -> 'descriptionLong'】】】】】】
                "2. 为这张图写一段30字'简介' (descriptionLong)。语言凝练而温暖，富有诗意。用作家史铁生的文笔撰写，但不要出现“史铁生”的字样，也不要出现“这张照片”的字样。也可以有古文的韵味，适当使用排比、赋比兴等形式。不用具体地描绘照片内容，而是表达对照片的感受和意境的理解。"
                +
                "3. *严格* 按照 {\"title\": \"...\", \"descriptionLong\": \"...\"} 格式返回。" +
                "4. *绝对不要* 在 JSON 之外添加任何 Markdown、注释或 `json` 标识。";

        return Map.of(
                "model", "qwen-vl-plus",
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", prompt),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:image/jpeg;base64," + base64Image))))),
                "max_tokens", 400);
    }

    // ... (buildOpenAiCompatibleRequestBody 辅助方法不变) ...
    private Map<String, Object> buildOpenAiCompatibleRequestBody(String base64Image) {
        String prompt = "你是一位文学家和专业的摄影评论家。请为这张摄影作品写一段30字左右的评语，语言凝练而温暖，富有诗意。用作家史铁生的文笔撰写，但不要出现“史铁生”的字样，也不要出现“这张照片”的字样。也可以有古文的韵味，适当使用排比、赋比兴等形式。不用具体地描绘照片内容，而是表达对照片的感受和意境的理解。";
        return Map.of(
                "model", "qwen-vl-plus",
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", prompt),
                                Map.of(
                                        "type", "image_url",
                                        "image_url", Map.of(
                                                "url", "data:image/jpeg;base64," + base64Image))))),
                "max_tokens", 400);
    }

    /**
     * 【【【新增】】】: 辅助方法
     * 解析 OpenAI 兼容的响应
     */
    // 【【【【【【修复】】】】】】: 压制 JSON 解析时不可避免的类型转换警告
    @SuppressWarnings("unchecked")
    private String parseOpenAiCompatibleResponse(Map<String, Object> response) {
        try {
            // OpenAI 路径: choices[0].message.content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            // (打印调试信息)
            System.err.println("解析 Qwen (OpenAI-mode) 响应失败: " + (response != null ? response.toString() : "null"));
            return "AI 响应解析失败 (Qwen-Compat)";
        }

    }
    // 在 AiService.java 中

    /**
     * 【【【【【【新增：JSON 清洁工】】】】】】
     * 无论 AI 在 JSON 外面包裹了什么 (Markdown, 聊天等),
     * 这个方法都会尝试提取出第一个 '{' 和最后一个 '}' 之间的内容。
     */
    private String extractJsonBlock(String rawResponse) {
        int firstBrace = rawResponse.indexOf('{');
        int lastBrace = rawResponse.lastIndexOf('}');

        // 确保我们找到了一个有效的 { ... } 块
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return rawResponse.substring(firstBrace, lastBrace + 1);
        }

        // 如果找不到 { } 块, 就返回原始响应 (让它在下一步中失败, 以便我们能看到原始错误)
        return rawResponse;
    }

}