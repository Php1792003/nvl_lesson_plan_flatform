package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.share.LessonPlanShareRepository;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonPlanAccessServiceTest {

    @Mock
    private LessonPlanShareRepository shareRepository;

    private LessonPlanAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new LessonPlanAccessService(shareRepository);
    }

    private CurrentUser createUser(Long id, String email, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setLastName("Nguyen");
        u.setGivenName("An");
        u.setStatus(UserStatus.ACTIVE);
        return new CurrentUser(u, Set.of(new SimpleGrantedAuthority(role)));
    }

    private LessonPlan createPlan(Long planId, Long ownerId) {
        LessonPlan plan = new LessonPlan();
        plan.setId(planId);
        plan.setTitle("Bài 1");
        if (ownerId != null) {
            User owner = new User();
            owner.setId(ownerId);
            plan.setOwner(owner);
        }
        return plan;
    }

    @Test
    @DisplayName("Chủ giáo án có toàn quyền xem và quản lý")
    void ownerCanViewAndManage() {
        CurrentUser owner = createUser(1L, "owner@school.edu.vn", "ROLE_TEACHER");
        LessonPlan plan = createPlan(10L, 1L);

        assertTrue(accessService.canView(owner, plan));
        assertTrue(accessService.canManage(owner, plan));
        assertDoesNotThrow(() -> accessService.requireViewAccess(owner, plan));
        assertDoesNotThrow(() -> accessService.requireManageAccess(owner, plan));
    }

    @Test
    @DisplayName("BGH có quyền xem tất cả giáo án nhưng không có quyền sửa (canManage = false)")
    void adminCanViewAllButCannotManage() {
        CurrentUser admin = createUser(2L, "admin@school.edu.vn", "ROLE_ADMIN");
        LessonPlan plan = createPlan(10L, 1L);

        assertTrue(accessService.canView(admin, plan));
        assertFalse(accessService.canManage(admin, plan));
    }

    @Test
    @DisplayName("Người được chia sẻ hợp lệ xem được nhưng không có quyền quản lý")
    void sharedUserCanViewOnly() {
        CurrentUser colleague = createUser(3L, "colleague@school.edu.vn", "ROLE_TEACHER");
        LessonPlan plan = createPlan(10L, 1L);

        when(shareRepository.hasActiveShare(10L, 3L)).thenReturn(true);

        assertTrue(accessService.canView(colleague, plan));
        assertFalse(accessService.canManage(colleague, plan));
    }

    @Test
    @DisplayName("Người lạ không có quyền xem sẽ nhận 404 (ResourceNotFoundException)")
    void strangerCannotViewThrows404() {
        CurrentUser stranger = createUser(4L, "stranger@school.edu.vn", "ROLE_TEACHER");
        LessonPlan plan = createPlan(10L, 1L);

        when(shareRepository.hasActiveShare(10L, 4L)).thenReturn(false);

        assertFalse(accessService.canView(stranger, plan));
        assertThrows(ResourceNotFoundException.class, () -> accessService.requireViewAccess(stranger, plan));
    }

    @Test
    @DisplayName("Giáo án mồ côi (owner == null): chỉ BGH xem được, không ai được sửa/xoá")
    void orphanPlanHandling() {
        CurrentUser teacher = createUser(5L, "teacher@school.edu.vn", "ROLE_TEACHER");
        CurrentUser admin = createUser(2L, "admin@school.edu.vn", "ROLE_ADMIN");
        LessonPlan orphanPlan = createPlan(10L, null);

        assertFalse(accessService.canView(teacher, orphanPlan));
        assertTrue(accessService.canView(admin, orphanPlan));

        assertFalse(accessService.canManage(teacher, orphanPlan));
        assertFalse(accessService.canManage(admin, orphanPlan));
    }
}
