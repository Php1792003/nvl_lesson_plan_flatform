package com.school.lessonplan.feedback;

import com.school.lessonplan.feedback.dto.FeedbackCreateForm;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String feedbackPage(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (!model.containsAttribute("feedbackForm")) {
            FeedbackCreateForm form = new FeedbackCreateForm();
            if (currentUser != null) {
                User user = userRepository.findById(currentUser.getId()).orElse(null);
                if (user != null) {
                    form.setSenderName(user.getFullName());
                    form.setSenderEmail(user.getEmail());
                    if (user.getDepartment() != null) {
                        form.setDepartmentName(user.getDepartment().getName());
                    }
                }
            }
            model.addAttribute("feedbackForm", form);
        }

        loadFeedbackLookups(model, currentUser);

        if (currentUser != null) {
            List<Feedback> myFeedbacks = feedbackService.getTeacherFeedbacks(currentUser.getId(), currentUser.getUsername());
            model.addAttribute("myFeedbacks", myFeedbacks);
            return "feedback/feedback-portal";
        } else {
            return "feedback/feedback-public";
        }
    }

    @PostMapping
    public String submitFeedback(@AuthenticationPrincipal CurrentUser currentUser,
                                 @Valid @ModelAttribute("feedbackForm") FeedbackCreateForm feedbackForm,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadFeedbackLookups(model, currentUser);
            if (currentUser != null) {
                List<Feedback> myFeedbacks = feedbackService.getTeacherFeedbacks(currentUser.getId(), currentUser.getUsername());
                model.addAttribute("myFeedbacks", myFeedbacks);
                return "feedback/feedback-portal";
            } else {
                return "feedback/feedback-public";
            }
        }

        Long userId = (currentUser != null) ? currentUser.getId() : null;
        feedbackService.createFeedback(feedbackForm, userId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Ý kiến đóng góp của Thầy/Cô đã được gửi đến Ban Giám Hiệu thành công. Nhà trường sẽ xem xét và phản hồi trong thời gian sớm nhất.");
        return "redirect:/feedback";
    }

    private void loadFeedbackLookups(Model model, CurrentUser currentUser) {
        model.addAttribute("categories", FeedbackCategory.values());
        model.addAttribute("priorities", FeedbackPriority.values());
        model.addAttribute("pageTitle", "Góp ý & Báo lỗi");
        model.addAttribute("isLoggedIn", currentUser != null);
    }
}
