package com.school.lessonplan.dashboard;

import com.school.lessonplan.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final com.school.lessonplan.security.ActiveUserService activeUserService;

    public DashboardController(DashboardService dashboardService,
            com.school.lessonplan.security.ActiveUserService activeUserService) {
        this.dashboardService = dashboardService;
        this.activeUserService = activeUserService;
    }

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isAdmin = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));

        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            Map<String, Object> stats = dashboardService.getAdminDashboardStats();
            model.addAllAttributes(stats);
        } else {
            Map<String, Object> stats = dashboardService.getTeacherDashboardStats(currentUser);
            model.addAllAttributes(stats);
        }

        return "dashboard/dashboard-home";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardData(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        boolean isAdmin = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));

        Map<String, Object> stats = isAdmin
                ? dashboardService.getAdminDashboardStats()
                : dashboardService.getTeacherDashboardStats(currentUser);

        return ResponseEntity.ok(stats);
    }

    /**
     * API Giám sát người dùng trực tuyến theo thời gian thực (Dành riêng cho Ban
     * Giám Hiệu)
     */
    @GetMapping("/api/dashboard/active-users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActiveUsers(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        boolean isAdmin = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).build();
        }

        long count = activeUserService.getActiveCount();
        List<com.school.lessonplan.security.ActiveUserService.ActiveUserInfo> users = activeUserService
                .getActiveUsersList();
        return ResponseEntity.ok(Map.of(
                "count", count,
                "users", users));
    }

    /**
     * Heartbeat ghi nhận hiện diện người dùng từ trình duyệt
     */
    @org.springframework.web.bind.annotation.RequestMapping(value = "/api/dashboard/heartbeat", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST })
    @ResponseBody
    public ResponseEntity<Map<String, String>> heartbeat(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
