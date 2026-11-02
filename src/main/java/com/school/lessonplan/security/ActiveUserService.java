package com.school.lessonplan.security;

import com.school.lessonplan.role.Role;
import com.school.lessonplan.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service quản lý và giám sát người dùng đang truy cập hệ thống theo thời gian thực (Active/Online Users).
 * Sử dụng cửa sổ trượt (sliding window) 5 phút để xác định người dùng đang thực tế hoạt động.
 */
@Service
public class ActiveUserService {

    private static final Logger log = LoggerFactory.getLogger(ActiveUserService.class);

    // Khoảng thời gian không có hoạt động để tính là offline (5 phút)
    private static final Duration ONLINE_TIMEOUT = Duration.ofMinutes(5);

    public record ActiveUserInfo(
        Long userId,
        String fullName,
        String email,
        String roleName,
        String departmentName,
        String campusName,
        Instant lastActiveAt,
        String lastUri,
        String lastPageTitle
    ) {}

    private final Map<Long, ActiveUserInfo> activeUsers = new ConcurrentHashMap<>();

    /**
     * Ghi nhận hoạt động của người dùng (An toàn tuyệt đối, không ném exception làm gián đoạn request)
     */
    public void recordActivity(User user, String uri) {
        if (user == null || user.getId() == null) {
            return;
        }

        try {
            ActiveUserInfo existing = activeUsers.get(user.getId());

            // Nếu URI là heartbeat hoặc api poll ngầm, giữ lại lastUri và lastPageTitle gần nhất
            String effectiveUri = uri;
            String effectiveTitle = resolvePageTitle(uri);
            if (isBackgroundApi(uri) && existing != null) {
                effectiveUri = existing.lastUri();
                effectiveTitle = existing.lastPageTitle();
            }

            String roleName = "Giáo viên";
            try {
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    roleName = user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
                }
            } catch (Exception ex) {
                if (existing != null) roleName = existing.roleName();
            }

            String deptName = "Chưa phân môn";
            try {
                if (user.getDepartment() != null) {
                    deptName = user.getDepartment().getName();
                }
            } catch (Exception ex) {
                if (existing != null) deptName = existing.departmentName();
            }

            String campusName = "Điểm trường chính";
            try {
                if (user.getCampus() != null) {
                    campusName = user.getCampus().getName();
                }
            } catch (Exception ex) {
                if (existing != null) campusName = existing.campusName();
            }

            activeUsers.put(user.getId(), new ActiveUserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roleName,
                deptName,
                campusName,
                Instant.now(),
                effectiveUri,
                effectiveTitle
            ));
        } catch (Exception e) {
            log.warn("Không thể ghi nhận hoạt động người dùng [{}]: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * Xoá người dùng khi họ đăng xuất (logout)
     */
    public void removeUser(Long userId) {
        if (userId != null) {
            activeUsers.remove(userId);
        }
    }

    /**
     * Lấy số lượng người dùng đang trực tuyến thực tế
     */
    public long getActiveCount() {
        cleanExpiredUsers();
        return activeUsers.size();
    }

    /**
     * Lấy danh sách chi tiết các tài khoản đang trực tuyến
     */
    public List<ActiveUserInfo> getActiveUsersList() {
        cleanExpiredUsers();
        return activeUsers.values().stream()
            .sorted(Comparator.comparing(ActiveUserInfo::lastActiveAt).reversed())
            .toList();
    }

    private void cleanExpiredUsers() {
        Instant threshold = Instant.now().minus(ONLINE_TIMEOUT);
        activeUsers.entrySet().removeIf(entry -> entry.getValue().lastActiveAt().isBefore(threshold));
    }

    private boolean isBackgroundApi(String uri) {
        if (uri == null) return false;
        return uri.contains("/heartbeat") || uri.contains("/active-users") || uri.contains("/stats");
    }

    private String resolvePageTitle(String uri) {
        if (uri == null) return "Hệ thống";
        if (uri.endsWith("/dashboard") || uri.endsWith("/")) return "Tổng quan";
        if (uri.contains("/plans/new")) return "Tải lên giáo án";
        if (uri.contains("/plans/edit")) return "Chỉnh sửa giáo án";
        if (uri.contains("/plans/")) return "Xem chi tiết giáo án";
        if (uri.contains("/plans")) return "Danh sách giáo án";
        if (uri.contains("/admin/users/new")) return "Thêm giáo viên mới";
        if (uri.contains("/admin/users/")) return "Chi tiết giáo viên";
        if (uri.contains("/admin/users")) return "Quản lý giáo viên";
        if (uri.contains("/admin/plans")) return "Toàn bộ giáo án (BGH)";
        if (uri.contains("/admin/roles")) return "Vai trò & Quyền";
        if (uri.contains("/admin/audit")) return "Nhật ký hệ thống";
        if (uri.contains("/admin/feedback")) return "Hòm thư góp ý (BGH)";
        if (uri.contains("/feedback")) return "Góp ý & Báo lỗi";
        if (uri.contains("/profile")) return "Hồ sơ cá nhân";
        if (uri.contains("/notifications")) return "Thông báo";
        return uri;
    }
}
