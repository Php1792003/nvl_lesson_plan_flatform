package com.school.lessonplan.share;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanAccessService;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.mail.EmailService;
import com.school.lessonplan.notification.Notification;
import com.school.lessonplan.notification.NotificationRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShareService {

    private final LessonPlanShareRepository shareRepository;
    private final LessonPlanRepository lessonPlanRepository;
    private final UserRepository userRepository;
    private final LessonPlanAccessService accessService;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    public ShareService(LessonPlanShareRepository shareRepository,
                        LessonPlanRepository lessonPlanRepository,
                        UserRepository userRepository,
                        LessonPlanAccessService accessService,
                        EmailService emailService,
                        NotificationRepository notificationRepository,
                        AuditService auditService) {
        this.shareRepository = shareRepository;
        this.lessonPlanRepository = lessonPlanRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
    }

    public List<LessonPlanShare> getActiveShares(Long planId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));
        accessService.requireManageAccess(user, plan);
        return shareRepository.findActiveSharesByPlanId(planId);
    }

    public Page<LessonPlanShare> getSharedWithMe(CurrentUser user, Pageable pageable) {
        return shareRepository.findSharedWithUser(user.getId(), pageable);
    }

    @Transactional
    public LessonPlanShare sharePlan(Long planId, CurrentUser user, String recipientEmail, Integer expireDays) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        // Chỉ chủ giáo án mới được chia sẻ
        accessService.requireManageAccess(user, plan);

        String cleanEmail = recipientEmail.trim().toLowerCase();
        User recipient = userRepository.findByEmail(cleanEmail)
            .orElseThrow(() -> new FieldValidationException("email", "NotFound", "Không tìm thấy giáo viên với email: " + cleanEmail));

        if (recipient.getId().equals(user.getId())) {
            throw new FieldValidationException("email", "Self", "Không thể chia sẻ giáo án cho chính mình.");
        }

        // Kiểm tra xem đã chia sẻ chưa
        if (shareRepository.hasActiveShare(planId, recipient.getId())) {
            throw new FieldValidationException("email", "Duplicate", "Giáo án đã được chia sẻ cho đồng nghiệp này.");
        }

        User granter = userRepository.findById(user.getId()).orElse(null);

        LessonPlanShare share = new LessonPlanShare();
        share.setPlan(plan);
        share.setSharedWithUser(recipient);
        share.setPermission("VIEW");
        share.setGrantedBy(granter);
        share.setGrantedAt(Instant.now());

        if (expireDays != null && expireDays > 0) {
            share.setExpiresAt(Instant.now().plus(expireDays, ChronoUnit.DAYS));
        }

        share = shareRepository.save(share);

        // Gửi email thông báo
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientName", recipient.getFullName());
        payload.put("granterName", granter != null ? granter.getFullName() : "Đồng nghiệp");
        payload.put("planTitle", plan.getTitle());
        payload.put("planId", plan.getId());
        payload.put("expiresAt", share.getExpiresAt() != null ? share.getExpiresAt().toString() : null);
        emailService.queueEmail(recipient.getEmail(), "Giáo án được chia sẻ: " + plan.getTitle(), "mail/mail-plan-shared", payload);

        // Tạo thông báo in-app
        Notification notif = new Notification();
        notif.setUser(recipient);
        notif.setType("PLAN_SHARED");
        notif.setTitle("Đồng nghiệp chia sẻ giáo án: " + plan.getTitle());
        notif.setLink("/plans/" + plan.getId());
        notificationRepository.save(notif);

        auditService.log("PLAN_SHARE", "LESSON_PLAN", plan.getId(),
            "Chia sẻ giáo án cho " + recipient.getEmail());

        return share;
    }

    @Transactional
    public void revokeShare(Long planId, Long shareId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireManageAccess(user, plan);

        LessonPlanShare share = shareRepository.findById(shareId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin chia sẻ."));

        if (!share.getPlan().getId().equals(planId)) {
            throw new ResourceNotFoundException("Thông tin chia sẻ không thuộc giáo án này.");
        }

        share.setRevokedAt(Instant.now());
        shareRepository.save(share);

        auditService.log("PLAN_REVOKE_SHARE", "LESSON_PLAN", plan.getId(),
            "Thu hồi chia sẻ giáo án với " + share.getSharedWithUser().getEmail());
    }
}
