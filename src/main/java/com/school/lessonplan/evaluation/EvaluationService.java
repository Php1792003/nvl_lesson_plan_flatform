package com.school.lessonplan.evaluation;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import com.school.lessonplan.lessonplan.LessonPlanVersionRepository;
import com.school.lessonplan.mail.EmailService;
import com.school.lessonplan.notification.Notification;
import com.school.lessonplan.notification.NotificationRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationReplyRepository replyRepository;
    private final LessonPlanRepository lessonPlanRepository;
    private final LessonPlanVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             EvaluationReplyRepository replyRepository,
                             LessonPlanRepository lessonPlanRepository,
                             LessonPlanVersionRepository versionRepository,
                             UserRepository userRepository,
                             NotificationRepository notificationRepository,
                             EmailService emailService,
                             AuditService auditService) {
        this.evaluationRepository = evaluationRepository;
        this.replyRepository = replyRepository;
        this.lessonPlanRepository = lessonPlanRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    /**
     * Chỉ chủ giáo án và BGH mới được xem đánh giá (mục 4.6)
     */
    public List<Evaluation> getEvaluationsForPlan(Long planId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        boolean isAdmin = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));

        boolean isOwner = plan.getOwner() != null && plan.getOwner().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            // Người được chia sẻ không thấy đánh giá
            return List.of();
        }

        return evaluationRepository.findByPlanIdOrderByCreatedAtDesc(planId);
    }

    /**
     * BGH tạo nhận xét mới trên một version của giáo án
     */
    @Transactional
    public Evaluation addEvaluation(Long planId, Long versionId, CurrentUser reviewerUser, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new FieldValidationException("content", "NotBlank", "Nội dung nhận xét không được để trống.");
        }

        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        LessonPlanVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản giáo án."));

        if (!version.getPlan().getId().equals(planId)) {
            throw new BusinessException("Phiên bản không khớp với giáo án.");
        }

        User reviewer = userRepository.findById(reviewerUser.getId()).orElse(null);

        Evaluation eval = new Evaluation();
        eval.setPlan(plan);
        eval.setVersion(version);
        eval.setReviewer(reviewer);
        eval.setReviewerNameSnapshot(reviewerUser.getFullName());
        eval.setContent(content.trim());
        eval = evaluationRepository.save(eval);

        // Nếu giáo án không phải mồ côi (owner != null) -> gửi email và notification
        if (plan.getOwner() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ownerName", plan.getOwner().getFullName());
            payload.put("reviewerName", reviewerUser.getFullName());
            payload.put("planTitle", plan.getTitle());
            payload.put("versionNo", version.getVersionNo());
            payload.put("content", eval.getContent());
            payload.put("planId", plan.getId());

            emailService.queueEmail(
                plan.getOwner().getEmail(),
                "Nhận xét mới cho giáo án: " + plan.getTitle(),
                "mail/mail-new-evaluation",
                payload
            );

            Notification notif = new Notification();
            notif.setUser(plan.getOwner());
            notif.setType("NEW_EVALUATION");
            notif.setTitle("Ban Giám Hiệu gửi nhận xét cho giáo án: " + plan.getTitle());
            notif.setLink("/plans/" + plan.getId());
            notificationRepository.save(notif);
        }

        auditService.log("EVALUATION_CREATE", "EVALUATION", eval.getId(),
            "Ban Giám Hiệu nhận xét giáo án: " + plan.getTitle() + " (phiên bản " + version.getVersionNo() + ")");

        return eval;
    }

    /**
     * BGH sửa nhận xét của chính mình (hiển thị 'đã chỉnh sửa', không gửi lại email)
     */
    @Transactional
    public Evaluation editEvaluation(Long evaluationId, CurrentUser reviewerUser, String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new FieldValidationException("content", "NotBlank", "Nội dung nhận xét không được để trống.");
        }

        Evaluation eval = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhận xét."));

        // Chỉ người viết nhận xét hoặc admin mới sửa được
        if (eval.getReviewer() != null && !eval.getReviewer().getId().equals(reviewerUser.getId())) {
            throw new BusinessException("Bạn chỉ có thể chỉnh sửa nhận xét của chính mình.");
        }

        eval.setContent(newContent.trim());
        eval.setEditedAt(Instant.now());
        eval = evaluationRepository.save(eval);

        auditService.log("EVALUATION_EDIT", "EVALUATION", eval.getId(), "Chỉnh sửa nội dung nhận xét");
        return eval;
    }

    /**
     * BGH thu hồi nhận xét của chính mình (hoặc Admin thu hồi)
     */
    @Transactional
    public void deleteEvaluation(Long evaluationId, CurrentUser reviewerUser) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhận xét."));

        boolean isAdmin = reviewerUser != null && reviewerUser.getAuthorities() != null &&
            reviewerUser.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));

        // Chỉ người viết nhận xét hoặc admin mới thu hồi được
        if (!isAdmin && eval.getReviewer() != null && reviewerUser != null && !eval.getReviewer().getId().equals(reviewerUser.getId())) {
            throw new BusinessException("Bạn chỉ có thể thu hồi nhận xét của chính mình.");
        }

        LessonPlan plan = eval.getPlan();
        String planTitle = plan != null ? plan.getTitle() : ("ID " + evaluationId);

        // Xoá các replies liên quan trước nếu có
        if (eval.getReplies() != null && !eval.getReplies().isEmpty()) {
            replyRepository.deleteAll(eval.getReplies());
        }

        evaluationRepository.delete(eval);

        auditService.log("EVALUATION_DELETE", "EVALUATION", evaluationId,
            "Thu hồi nhận xét trên giáo án: " + planTitle);
    }

    /**
     * Chủ giáo án trả lời nhận xét của BGH
     */
    @Transactional
    public EvaluationReply replyToEvaluation(Long evaluationId, CurrentUser authorUser, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new FieldValidationException("content", "NotBlank", "Nội dung phản hồi không được để trống.");
        }

        Evaluation eval = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhận xét."));

        LessonPlan plan = eval.getPlan();

        // Chỉ chủ giáo án mới được trả lời nhận xét
        if (plan.getOwner() == null || !plan.getOwner().getId().equals(authorUser.getId())) {
            throw new BusinessException("Chỉ chủ giáo án mới có quyền gửi phản hồi.");
        }

        User author = userRepository.findById(authorUser.getId()).orElse(null);

        EvaluationReply reply = new EvaluationReply();
        reply.setEvaluation(eval);
        reply.setAuthor(author);
        reply.setAuthorNameSnapshot(authorUser.getFullName());
        reply.setContent(content.trim());
        reply = replyRepository.save(reply);

        // Gửi email & notification cho người đã viết nhận xét (nếu người đó còn tồn tại)
        if (eval.getReviewer() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reviewerName", eval.getReviewerNameSnapshot());
            payload.put("authorName", authorUser.getFullName());
            payload.put("planTitle", plan.getTitle());
            payload.put("content", reply.getContent());
            payload.put("planId", plan.getId());

            emailService.queueEmail(
                eval.getReviewer().getEmail(),
                "Phản hồi nhận xét giáo án: " + plan.getTitle(),
                "mail/mail-evaluation-reply",
                payload
            );

            Notification notif = new Notification();
            notif.setUser(eval.getReviewer());
            notif.setType("EVALUATION_REPLY");
            notif.setTitle("Giáo viên phản hồi nhận xét trên giáo án: " + plan.getTitle());
            notif.setLink("/plans/" + plan.getId());
            notificationRepository.save(notif);
        }

        auditService.log("EVALUATION_REPLY", "EVALUATION_REPLY", reply.getId(),
            "Giáo viên trả lời nhận xét trên giáo án: " + plan.getTitle());

        return reply;
    }
}
