package com.school.lessonplan.common.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(Model model) {
        model.addAttribute("message", "Bạn không có quyền xem hoặc thực hiện thao tác trên trang này.");
        return "error/403";
    }

    @GetMapping("/error/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("message", "Tài nguyên không tồn tại.");
        return "error/404";
    }

    @GetMapping("/error/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(Model model) {
        model.addAttribute("message", "Đã xảy ra lỗi máy chủ. Vui lòng thử lại sau.");
        return "error/500";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());

                if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    return accessDenied(model);
                } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    return notFound(model);
                } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                    model.addAttribute("message", "Yêu cầu không hợp lệ.");
                    return "error/400";
                } else if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
                    model.addAttribute("message", "Quá nhiều yêu cầu. Vui lòng thử lại sau.");
                    return "error/429";
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return serverError(model);
    }
}
