package com.school.lessonplan.feedback;

import com.school.lessonplan.feedback.dto.FeedbackResponseForm;
import com.school.lessonplan.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/feedbacks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String listFeedbacks(
            @RequestParam(name = "status", required = false) FeedbackStatus status,
            @RequestParam(name = "category", required = false) FeedbackCategory category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model) {

        Page<Feedback> feedbackPage = feedbackService.searchFeedbacks(
                status, category, keyword, PageRequest.of(page, size)
        );

        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("stats", feedbackService.getFeedbackStats());
        model.addAttribute("categories", FeedbackCategory.values());
        model.addAttribute("statuses", FeedbackStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Hòm thư Góp ý & Báo lỗi");

        return "admin/feedback/admin-feedback-list";
    }

    @PostMapping("/{id}/respond")
    public String respondFeedback(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @ModelAttribute("responseForm") FeedbackResponseForm responseForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu phản hồi không hợp lệ.");
            return "redirect:/admin/feedbacks";
        }

        Long adminId = (currentUser != null) ? currentUser.getId() : null;
        feedbackService.respondFeedback(id, responseForm, adminId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã cập nhật trạng thái và gửi phản hồi thành công cho ý kiến #" + id);
        return "redirect:/admin/feedbacks";
    }

    @PostMapping("/{id}/delete")
    public String deleteFeedback(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        Long adminId = (currentUser != null) ? currentUser.getId() : null;
        feedbackService.deleteFeedback(id, adminId);

        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ý kiến đóng góp #" + id);
        return "redirect:/admin/feedbacks";
    }
}
