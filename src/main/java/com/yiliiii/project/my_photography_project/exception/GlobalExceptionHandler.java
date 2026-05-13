package com.yiliiii.project.my_photography_project.exception; // 1. 你的包名

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// 【新增】 导入日志库
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 【新增】 全局异常处理器
 * @ControllerAdvice 注解使其成为一个"全局"组件,
 * 它会"监听"所有 @Controller 抛出的异常。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // (最佳实践: 添加一个日志记录器, 在后台记录完整的错误)
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. 捕获 *特定* 的异常 (e.g., 我们自己抛出的 "ID 无效" 异常)
     * 当 Controller/Service 抛出 IllegalArgumentException 时, 此方法被调用。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        
        // 1. 在后台记录详细错误 (给开发者看)
        logger.warn("请求参数无效: {}", ex.getMessage());
        
        // 2. 向前端传递一个 "友好" 的信息 (给用户看)
        model.addAttribute("errorMessage", "对不起，我们没有找到您要查找的资源。");
        
        // 3. 返回 error.html 视图
        return "error";
    }

    /**
     * 2. 捕获 *所有其他* 的未知异常 (e.g., 数据库连接失败, NullPointerException)
     * 这是最后的"兜底"防线。
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        
        // 1. 在后台记录 *严重* 错误 (给开发者看)
        logger.error("发生了一个未知的严重错误:", ex);
        
        // 2. 向前端传递一个 "通用" 的信息 (给用户看)
        model.addAttribute("errorMessage", "服务器开小差了，请稍后重试或联系管理员。");
        
        // 3. 返回 error.html 视图
        return "error";
    }
}