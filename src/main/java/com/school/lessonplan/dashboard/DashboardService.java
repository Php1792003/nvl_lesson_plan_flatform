package com.school.lessonplan.dashboard;

import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.lessonplan.LessonPlanVersionRepository;
import com.school.lessonplan.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private final LessonPlanRepository lessonPlanRepository;
    private final LessonPlanVersionRepository versionRepository;

    public DashboardService(LessonPlanRepository lessonPlanRepository,
            LessonPlanVersionRepository versionRepository) {
        this.lessonPlanRepository = lessonPlanRepository;
        this.versionRepository = versionRepository;
    }

    public Map<String, Object> getTeacherDashboardStats(CurrentUser user) {
        Map<String, Object> stats = new HashMap<>();
        long totalPlans = lessonPlanRepository.countByOwnerId(user.getId());
        stats.put("totalPlans", totalPlans);

        List<LessonPlan> myPlans = lessonPlanRepository.findAllByOwnerId(user.getId());

        // Thống kê 12 tháng gần nhất
        Map<String, Long> monthlyStats = computeMonthlyCounts(myPlans);
        stats.put("monthlyLabels", new ArrayList<>(monthlyStats.keySet()));
        stats.put("monthlyData", new ArrayList<>(monthlyStats.values()));

        return stats;
    }

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalPlans = lessonPlanRepository.countAll();
        long storageBytes = versionRepository.getTotalActiveStorageBytes();
        long purgedCount = versionRepository.getTotalPurgedVersionsCount();

        stats.put("totalPlans", totalPlans);
        stats.put("storageBytes", storageBytes);
        stats.put("storageMb", String.format("%.2f", (double) storageBytes / (1024 * 1024)));
        stats.put("purgedVersionsCount", purgedCount);

        List<LessonPlan> allPlans = lessonPlanRepository.findAll();

        // Thống kê 12 tháng gần nhất toàn trường (kể cả giáo án mồ côi)
        Map<String, Long> monthlyStats = computeMonthlyCounts(allPlans);
        stats.put("monthlyLabels", new ArrayList<>(monthlyStats.keySet()));
        stats.put("monthlyData", new ArrayList<>(monthlyStats.values()));

        // Thống kê theo bộ môn
        List<Object[]> deptCounts = lessonPlanRepository.countByDepartment();
        List<String> deptLabels = new ArrayList<>();
        List<Long> deptData = new ArrayList<>();
        for (Object[] row : deptCounts) {
            deptLabels.add((String) row[0]);
            deptData.add(row[1] != null ? ((Number) row[1]).longValue() : 0L);
        }
        stats.put("deptLabels", deptLabels);
        stats.put("deptData", deptData);

        return stats;
    }

    private Map<String, Long> computeMonthlyCounts(List<LessonPlan> plans) {
        Map<String, Long> counts = new LinkedHashMap<>();
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth current = YearMonth.now(zone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        // Khởi tạo 12 tháng gần nhất
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            counts.put(ym.format(formatter), 0L);
        }

        for (LessonPlan plan : plans) {
            if (plan.getCreatedAt() != null) {
                YearMonth planYm = YearMonth.from(plan.getCreatedAt().atZone(zone));
                String key = planYm.format(formatter);
                if (counts.containsKey(key)) {
                    counts.put(key, counts.get(key) + 1);
                }
            }
        }

        return counts;
    }
}
