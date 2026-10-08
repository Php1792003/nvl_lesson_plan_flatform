package com.school.lessonplan.notification;

import com.school.lessonplan.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public String listNotifications(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        if (currentUser == null) {
            return "redirect:/login";
        }

        Page<Notification> notifPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            currentUser.getId(), PageRequest.of(page, size)
        );

        model.addAttribute("notifPage", notifPage);
        return "notification/notification-inbox";
    }

    @PostMapping("/mark-all-read")
    @Transactional
    public String markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser != null) {
            notificationRepository.markAllAsRead(currentUser.getId());
        }
        return "redirect:/notifications";
    }
}
