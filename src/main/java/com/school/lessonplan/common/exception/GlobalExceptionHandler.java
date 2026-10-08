package com.school.lessonplan.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request, Model model) {
        String path = ex.getResourcePath();
        if (isStaticAsset(path) || !isHtmlRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        log.debug("Page resource not found: {}", path);
        model.addAttribute("message", "Tài nguyên không tồn tại.");
        return "error/404";
    }

    private boolean isStaticAsset(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.endsWith(".map") || lower.endsWith(".json") || lower.endsWith(".ico")
                || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".svg") || lower.endsWith(".css") || lower.endsWith(".js")
                || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf")
                || lower.startsWith(".well-known/") || lower.startsWith("well-known/");
    }

    private boolean isHtmlRequest(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, Model model) {
        log.warn("Access denied handled in GlobalExceptionHandler: {}", ex.getMessage());
        model.addAttribute("message", "Bạn không có quyền thực hiện thao tác này.");
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessError(BusinessException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public String handleRateLimit(RateLimitException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/429";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, Model model) {
        model.addAttribute("message", "File quá lớn. Giới hạn tối đa là 10 MB.");
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unexpected error: ", ex);
        model.addAttribute("message", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
        return "error/500";
    }
}
