package com.school.lessonplan.lessonplan;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.EvaluationRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import com.school.lessonplan.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonPlanServiceTest {

    @Mock private LessonPlanRepository lessonPlanRepository;
    @Mock private LessonPlanVersionRepository versionRepository;
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private LessonPlanAccessService accessService;
    @Mock private FileStorageService fileStorageService;
    @Mock private AuditService auditService;

    private LessonPlanService lessonPlanService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        lessonPlanService = new LessonPlanService(
            lessonPlanRepository, versionRepository, evaluationRepository,
            departmentRepository, userRepository, accessService,
            fileStorageService, auditService, appProperties
        );
    }

    private CurrentUser createCurrentUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("test@school.edu.vn");
        u.setStatus(UserStatus.ACTIVE);
        return new CurrentUser(u, Set.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("Quy tắc 4.5b: Giáo án ĐÃ CÓ đánh giá từ BGH thì KHÔNG ĐƯỢC XOÁ")
    void cannotDeletePlanWithEvaluations() {
        CurrentUser owner = createCurrentUser(1L);
        LessonPlan plan = new LessonPlan();
        plan.setId(10L);

        when(lessonPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        doNothing().when(accessService).requireManageAccess(owner, plan);
        when(evaluationRepository.existsByPlanId(10L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
            lessonPlanService.deletePlan(10L, owner)
        );

        assertTrue(ex.getMessage().contains("không thể xoá"));
        verify(lessonPlanRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Quy tắc 4.5b: Giáo án CHƯA CÓ đánh giá được phép xoá")
    void canDeletePlanWithoutEvaluations() {
        CurrentUser owner = createCurrentUser(1L);
        LessonPlan plan = new LessonPlan();
        plan.setId(10L);

        when(lessonPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        doNothing().when(accessService).requireManageAccess(owner, plan);
        when(evaluationRepository.existsByPlanId(10L)).thenReturn(false);

        assertDoesNotThrow(() -> lessonPlanService.deletePlan(10L, owner));
        verify(lessonPlanRepository, times(1)).delete(plan);
    }
}
