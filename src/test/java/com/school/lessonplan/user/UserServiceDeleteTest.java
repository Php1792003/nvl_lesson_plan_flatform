package com.school.lessonplan.user;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.auth.EmailVerificationCodeRepository;
import com.school.lessonplan.auth.PasswordResetTokenRepository;
import com.school.lessonplan.auth.RefreshTokenRepository;
import com.school.lessonplan.campus.Campus;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.common.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceDeleteTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private LessonPlanRepository lessonPlanRepository;
    @Mock private EvaluationReplyRepository replyRepository;
    @Mock private LessonPlanShareRepository shareRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationCodeRepository otpRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailOutboxRepository emailOutboxRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository, roleRepository, departmentRepository, campusRepository,
            lessonPlanRepository, replyRepository, shareRepository, refreshTokenRepository,
            otpRepository, resetTokenRepository, notificationRepository, emailOutboxRepository,
            emailService, auditService, passwordEncoder
        );
    }

    private CurrentUser createAdminActor(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("admin@school.edu.vn");
        u.setStatus(UserStatus.ACTIVE);
        return new CurrentUser(u, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Quy tắc 4.5a: Xoá giáo viên phải giữ lại giáo án với snapshot và gán owner = null")
    void deleteUserPreservesPlansWithSnapshot() {
        CurrentUser adminActor = createAdminActor(1L);

        User teacher = new User();
        teacher.setId(2L);
        teacher.setEmail("teacher@school.edu.vn");
        teacher.setLastName("Tran");
        teacher.setGivenName("Thi B");
        Department dept = new Department();
        dept.setName("Vật lý");
        teacher.setDepartment(dept);
        Campus campus = new Campus();
        campus.setName("Nguyễn Văn Linh");
        teacher.setCampus(campus);

        LessonPlan plan = new LessonPlan();
        plan.setId(100L);
        plan.setTitle("Giáo án Vật lý 10");
        plan.setOwner(teacher);

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(lessonPlanRepository.findAllByOwnerId(2L)).thenReturn(List.of(plan));

        // Thực hiện xoá với email xác nhận chính xác
        userService.deleteUser(2L, "teacher@school.edu.vn", adminActor);

        // Kiểm tra snapshot và owner = null
        ArgumentCaptor<LessonPlan> planCaptor = ArgumentCaptor.forClass(LessonPlan.class);
        verify(lessonPlanRepository).save(planCaptor.capture());

        LessonPlan savedPlan = planCaptor.getValue();
        assertNull(savedPlan.getOwner());
        assertEquals("Tran Thi B", savedPlan.getOwnerSnapshotName());
        assertEquals("teacher@school.edu.vn", savedPlan.getOwnerSnapshotEmail());
        assertEquals("Vật lý", savedPlan.getOwnerSnapshotDepartment());
        assertEquals("Nguyễn Văn Linh", savedPlan.getOwnerSnapshotCampus());

        // Kiểm tra dọn dẹp liên kết
        verify(replyRepository).nullifyAuthorByUserId(2L);
        verify(shareRepository).deleteBySharedWithUserId(2L);
        verify(shareRepository).nullifyGrantedBy(2L);
        verify(refreshTokenRepository).deleteByUserId(2L);
        verify(otpRepository).deleteByUserId(2L);
        verify(resetTokenRepository).deleteByUserId(2L);
        verify(notificationRepository).deleteByUserId(2L);
        verify(emailOutboxRepository).deletePendingByEmail("teacher@school.edu.vn");

        // Cuối cùng xoá user thật trong DB
        verify(userRepository).delete(teacher);
    }

    @Test
    @DisplayName("Không được tự xoá chính mình")
    void cannotDeleteSelf() {
        CurrentUser adminActor = createAdminActor(1L);
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@school.edu.vn");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
            userService.deleteUser(1L, "admin@school.edu.vn", adminActor)
        );
        assertTrue(ex.getMessage().contains("tự xoá chính mình"));
    }

    @Test
    @DisplayName("Không được xoá ADMIN cuối cùng trong hệ thống")
    void cannotDeleteLastAdmin() {
        CurrentUser actor = createAdminActor(99L);

        User targetAdmin = new User();
        targetAdmin.setId(2L);
        targetAdmin.setEmail("lastadmin@school.edu.vn");
        Role adminRole = new Role();
        adminRole.setCode("ADMIN");
        targetAdmin.setRoles(Set.of(adminRole));

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.countAdmins()).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
            userService.deleteUser(2L, "lastadmin@school.edu.vn", actor)
        );
        assertTrue(ex.getMessage().contains("cuối cùng"));
    }
}
