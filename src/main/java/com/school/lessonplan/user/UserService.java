package com.school.lessonplan.user;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.auth.EmailVerificationCodeRepository;
import com.school.lessonplan.auth.PasswordResetTokenRepository;
import com.school.lessonplan.auth.RefreshTokenRepository;
import com.school.lessonplan.campus.Campus;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.EvaluationReplyRepository;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.mail.EmailOutboxRepository;
import com.school.lessonplan.mail.EmailService;
import com.school.lessonplan.notification.NotificationRepository;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.share.LessonPlanShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final LessonPlanRepository lessonPlanRepository;
    private final EvaluationReplyRepository replyRepository;
    private final LessonPlanShareRepository shareRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationCodeRepository otpRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final NotificationRepository notificationRepository;
    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       DepartmentRepository departmentRepository,
                       CampusRepository campusRepository,
                       LessonPlanRepository lessonPlanRepository,
                       EvaluationReplyRepository replyRepository,
                       LessonPlanShareRepository shareRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       EmailVerificationCodeRepository otpRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       NotificationRepository notificationRepository,
                       EmailOutboxRepository emailOutboxRepository,
                       EmailService emailService,
                       AuditService auditService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.lessonPlanRepository = lessonPlanRepository;
        this.replyRepository = replyRepository;
        this.shareRepository = shareRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpRepository = otpRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.notificationRepository = notificationRepository;
        this.emailOutboxRepository = emailOutboxRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> searchUsers(String search, UserStatus status, Long departmentId, Long campusId, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return userRepository.searchUsers(cleanSearch, status, departmentId, campusId, pageable);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));
    }

    @Transactional
    public User createUser(String lastName, String givenName, String email, String password,
                           Long departmentId, Long campusId, List<Long> roleIds) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email đã được sử dụng.");
        }

        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));
        Campus campus = campusRepository.findById(campusId)
            .orElseThrow(() -> new BusinessException("Phân hiệu không tồn tại."));

        User user = new User();
        user.setLastName(lastName.trim());
        user.setGivenName(givenName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDepartment(department);
        user.setCampus(campus);
        user.setStatus(UserStatus.ACTIVE); // Tạo trực tiếp bởi BGH thì ACTIVE luôn

        if (roleIds != null && !roleIds.isEmpty()) {
            user.setRoles(new HashSet<>(roleRepository.findAllById(roleIds)));
        } else {
            Role teacherRole = roleRepository.findByCode("TEACHER")
                .orElseThrow(() -> new BusinessException("Vai trò TEACHER không tồn tại."));
            user.getRoles().add(teacherRole);
        }

        user = userRepository.save(user);
        auditService.log("USER_CREATE", "USER", user.getId(), "Tạo tài khoản: " + user.getEmail());
        return user;
    }

    @Transactional
    public User updateUser(Long id, String lastName, String givenName, Long departmentId,
                           Long campusId, List<Long> roleIds) {
        User user = getById(id);

        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));
        Campus campus = campusRepository.findById(campusId)
            .orElseThrow(() -> new BusinessException("Phân hiệu không tồn tại."));

        user.setLastName(lastName.trim());
        user.setGivenName(givenName.trim());
        user.setDepartment(department);
        user.setCampus(campus);

        if (roleIds != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            // Kiểm tra không gỡ bỏ ADMIN cuối cùng
            boolean wasAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
            boolean willBeAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
            if (wasAdmin && !willBeAdmin && userRepository.countAdmins() <= 1) {
                throw new BusinessException("Không thể gỡ vai trò ADMIN của tài khoản Ban Giám Hiệu cuối cùng.");
            }
            user.setRoles(roles);
            user.incrementTokenVersion(); // Đổi vai trò -> vô hiệu hoá token ngay
        }

        user = userRepository.save(user);
        auditService.log("USER_UPDATE", "USER", user.getId(), "Cập nhật tài khoản: " + user.getEmail());
        return user;
    }

    @Transactional
    public void approveUser(Long id) {
        User user = getById(id);
        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể phê duyệt tài khoản ở trạng thái chờ duyệt.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Gửi email thông báo phê duyệt
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", user.getFullName());
        emailService.queueEmail(user.getEmail(), "Tài khoản của bạn đã được kích hoạt", "mail/account-approved", payload);

        auditService.log("USER_APPROVE", "USER", user.getId(), "Phê duyệt tài khoản: " + user.getEmail());
    }

    @Transactional
    public void lockUser(Long id, String reason) {
        User user = getById(id);
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Lý do khoá tài khoản không được để trống.");
        }

        // Không khoá chính mình hoặc khoá admin cuối cùng
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (isAdmin && userRepository.countAdmins() <= 1) {
            throw new BusinessException("Không thể khoá tài khoản Ban Giám Hiệu cuối cùng.");
        }

        user.setStatus(UserStatus.LOCKED);
        user.setLockReason(reason.trim());
        user.setLockedAt(Instant.now());
        user.incrementTokenVersion(); // Mất hiệu lực ngay lập tức!
        userRepository.save(user);

        auditService.log("USER_LOCK", "USER", user.getId(), "Khoá tài khoản: " + user.getEmail() + ", lý do: " + reason);
    }

    @Transactional
    public void unlockUser(Long id) {
        User user = getById(id);
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new BusinessException("Tài khoản không ở trạng thái bị khoá.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setLockReason(null);
        user.setLockedAt(null);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditService.log("USER_UNLOCK", "USER", user.getId(), "Mở khoá tài khoản: " + user.getEmail());
    }

    /**
     * Xoá giáo viên tuân thủ nghiêm ngặt Quy tắc 4.5a:
     * - Giữ nguyên giáo án (cả file lẫn version).
     * - Ghi snapshot vào mọi giáo án: owner_snapshot_name, owner_snapshot_email,
     *   owner_snapshot_department, owner_snapshot_campus.
     * - lesson_plans.owner_id = NULL.
     * - evaluation_replies.author_id = NULL (giữ author_name_snapshot).
     * - lesson_plan_shares: xoá dòng shared_with_user_id = user.id; giữ nguyên share user đã cấp (granted_by = NULL).
     * - Xoá tokens, OTP, notifications, outbox pending.
     * - Ghi audit_logs (snapshot actor).
     * - Không cho xoá chính mình, không cho xoá ADMIN cuối cùng.
     */
    @Transactional
    public void deleteUser(Long id, String confirmEmail, CurrentUser actor) {
        User user = getById(id);

        if (confirmEmail == null || !confirmEmail.trim().equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("Email xác nhận không khớp với tài khoản cần xoá.");
        }

        if (actor != null && actor.getId().equals(id)) {
            throw new BusinessException("Bạn không thể tự xoá chính mình.");
        }

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (isAdmin && userRepository.countAdmins() <= 1) {
            throw new BusinessException("Không thể xoá tài khoản Ban Giám Hiệu cuối cùng trong hệ thống.");
        }

        String userEmail = user.getEmail();
        String userFullName = user.getFullName();
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "";
        String campusName = user.getCampus() != null ? user.getCampus().getName() : "";

        // 1. Snapshot thông tin chủ sở hữu vào mọi giáo án và set owner = null
        List<LessonPlan> ownedPlans = lessonPlanRepository.findAllByOwnerId(id);
        for (LessonPlan plan : ownedPlans) {
            plan.setOwnerSnapshotName(userFullName);
            plan.setOwnerSnapshotEmail(userEmail);
            plan.setOwnerSnapshotDepartment(deptName);
            plan.setOwnerSnapshotCampus(campusName);
            plan.setOwner(null);
            lessonPlanRepository.save(plan);
        }

        // 2. evaluation_replies.author_id = NULL
        replyRepository.nullifyAuthorByUserId(id);

        // 3. lesson_plan_shares
        shareRepository.deleteBySharedWithUserId(id);
        shareRepository.nullifyGrantedBy(id);

        // 4. Xoá refresh_tokens, email_verification_codes, password_reset_tokens, notifications, outbox
        refreshTokenRepository.deleteByUserId(id);
        otpRepository.deleteByUserId(id);
        resetTokenRepository.deleteByUserId(id);
        notificationRepository.deleteByUserId(id);
        emailOutboxRepository.deletePendingByEmail(userEmail);

        // 5. Ghi audit log
        auditService.log("USER_DELETE", "USER", id, "Xoá người dùng (giữ giáo án mồ côi): " + userEmail);

        // 6. Xoá user entity
        userRepository.delete(user);
        log.info("Deleted user {} - preserved {} orphan lesson plans", userEmail, ownedPlans.size());
    }

    @Transactional
    public void updateProfile(Long userId, String lastName, String givenName, Long departmentId, Long campusId) {
        User user = getById(userId);
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));
        Campus campus = campusRepository.findById(campusId)
            .orElseThrow(() -> new BusinessException("Phân hiệu không tồn tại."));

        user.setLastName(lastName.trim());
        user.setGivenName(givenName.trim());
        user.setDepartment(department);
        user.setCampus(campus);
        userRepository.save(user);

        auditService.log("USER_UPDATE_PROFILE", "USER", user.getId(), "Cập nhật hồ sơ cá nhân");
    }
}
