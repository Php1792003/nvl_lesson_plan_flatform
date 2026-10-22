package com.school.lessonplan.feedback;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.feedback.dto.FeedbackCreateForm;
import com.school.lessonplan.feedback.dto.FeedbackResponseForm;
import com.school.lessonplan.notification.Notification;
import com.school.lessonplan.notification.NotificationRepository;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           UserRepository userRepository,
                           NotificationRepository notificationRepository,
                           AuditService auditService) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Feedback createFeedback(FeedbackCreateForm form, Long currentUserId) {
        Feedback feedback = new Feedback();
        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId).orElse(null);
            feedback.setUser(user);
            if (user != null) {
                if (form.getSenderName() == null || form.getSenderName().isBlank()) {
                    feedback.setSenderName(user.getFullName());
                } else {
                    feedback.setSenderName(form.getSenderName());
                }
                if (form.getSenderEmail() == null || form.getSenderEmail().isBlank()) {
                    feedback.setSenderEmail(user.getEmail());
                } else {
                    feedback.setSenderEmail(form.getSenderEmail());
                }
                if (user.getDepartment() != null && (form.getDepartmentName() == null || form.getDepartmentName().isBlank())) {
                    feedback.setDepartmentName(user.getDepartment().getName());
                } else {
                    feedback.setDepartmentName(form.getDepartmentName());
                }
            }
        }

        if (feedback.getSenderName() == null) feedback.setSenderName(form.getSenderName());
        if (feedback.getSenderEmail() == null) feedback.setSenderEmail(form.getSenderEmail());
        if (feedback.getDepartmentName() == null) feedback.setDepartmentName(form.getDepartmentName());

        feedback.setTitle(form.getTitle());
        feedback.setCategory(form.getCategory());
        feedback.setPriority(form.getPriority());
        feedback.setContent(form.getContent());
        feedback.setStatus(FeedbackStatus.PENDING);

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Teacher feedback submitted ID: {} by {}", saved.getId(), saved.getSenderEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Feedback> getTeacherFeedbacks(Long userId, String email) {
        if (userId != null) {
            return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        if (email != null && !email.isBlank()) {
            return feedbackRepository.findBySenderEmailIgnoreCaseOrderByCreatedAtDesc(email.trim().toLowerCase());
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public Feedback getFeedbackById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ý kiến đóng góp với mã: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Feedback> searchFeedbacks(FeedbackStatus status, FeedbackCategory category, String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return feedbackRepository.searchFeedbacks(status, category, cleanKeyword, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getFeedbackStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", feedbackRepository.count());
        stats.put("pending", feedbackRepository.countByStatus(FeedbackStatus.PENDING));
        stats.put("inProgress", feedbackRepository.countByStatus(FeedbackStatus.IN_PROGRESS));
        stats.put("resolved", feedbackRepository.countByStatus(FeedbackStatus.RESOLVED));
        stats.put("closed", feedbackRepository.countByStatus(FeedbackStatus.CLOSED));
        return stats;
    }

    @Transactional
    public Feedback respondFeedback(Long feedbackId, FeedbackResponseForm form, Long adminUserId) {
        Feedback feedback = getFeedbackById(feedbackId);
        User admin = (adminUserId != null) ? userRepository.findById(adminUserId).orElse(null) : null;

        feedback.setStatus(form.getStatus());
        feedback.setAdminResponse(form.getAdminResponse());
        feedback.setRespondedBy(admin);
        feedback.setRespondedAt(Instant.now());

        Feedback saved = feedbackRepository.save(feedback);

        // Gửi thông báo đến giáo viên nếu tài khoản tồn tại trong hệ thống
        if (saved.getUser() != null) {
            try {
                Notification notif = new Notification();
                notif.setUser(saved.getUser());
                notif.setType("FEEDBACK_RESPONSE");
                notif.setTitle("Ban Giám Hiệu đã phản hồi góp ý: \"" + saved.getTitle() + "\" (" + saved.getStatus().getDisplayName() + ")");
                notif.setLink("/feedback");
                notificationRepository.save(notif);
            } catch (Exception ex) {
                log.warn("Could not create notification for feedback response: {}", ex.getMessage());
            }
        }

        auditService.log("FEEDBACK_RESPOND", "FEEDBACK", saved.getId(),
                "BGH phản hồi góp ý ID " + saved.getId() + ", trạng thái: " + saved.getStatus().name());

        return saved;
    }

    @Transactional
    public void deleteFeedback(Long feedbackId, Long adminUserId) {
        Feedback feedback = getFeedbackById(feedbackId);
        feedbackRepository.delete(feedback);
        auditService.log("FEEDBACK_DELETE", "FEEDBACK", feedbackId,
                "Xóa ý kiến đóng góp ID " + feedbackId);
    }
}
