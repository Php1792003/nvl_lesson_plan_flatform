package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.share.LessonPlanShareRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class LessonPlanAccessService {

    private final LessonPlanShareRepository shareRepository;

    public LessonPlanAccessService(LessonPlanShareRepository shareRepository) {
        this.shareRepository = shareRepository;
    }

    public boolean canView(CurrentUser user, LessonPlan plan) {
        if (user == null || plan == null) {
            return false;
        }

        // 1. User có PLAN_VIEW_ALL hoặc vai trò ADMIN
        boolean hasAdminAccess = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));
        if (hasAdminAccess) {
            return true;
        }

        // 2. User là chủ sở hữu giáo án
        if (plan.getOwner() != null && plan.getOwner().getId().equals(user.getId())) {
            return true;
        }

        // 3. User được chia sẻ giáo án hợp lệ (chưa thu hồi, chưa hết hạn)
        return shareRepository.hasActiveShare(plan.getId(), user.getId());
    }

    public boolean canManage(CurrentUser user, LessonPlan plan) {
        if (user == null || plan == null) {
            return false;
        }
        // Chỉ chủ sở hữu mới có quyền sửa, xoá, chia sẻ, thêm version
        // Giáo án mồ côi (owner == null) thì không ai sửa/xoá được
        return plan.getOwner() != null && plan.getOwner().getId().equals(user.getId());
    }

    /**
     * Bắt buộc có quyền xem, nếu không trả 404 (chống IDOR và không lộ tài nguyên)
     */
    public void requireViewAccess(CurrentUser user, LessonPlan plan) {
        if (!canView(user, plan)) {
            throw new ResourceNotFoundException("Không tìm thấy giáo án.");
        }
    }

    /**
     * Bắt buộc là chủ sở hữu, nếu không trả 404
     */
    public void requireManageAccess(CurrentUser user, LessonPlan plan) {
        if (!canManage(user, plan)) {
            throw new ResourceNotFoundException("Không tìm thấy giáo án.");
        }
    }
}
