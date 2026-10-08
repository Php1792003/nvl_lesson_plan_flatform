package com.school.lessonplan.dashboard;

import com.school.lessonplan.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
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

        return "dashboard/index";
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
}
