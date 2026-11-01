package com.school.lessonplan.nq52;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Nq52CriteriaCatalog {

    public record CriteriaItem(String code, String label, double maxScore, String groupCode, String subgroup) {}
    public record RadioOption(String key, String label, double score) {}

    // Danh mục tiêu chí Phần A
    public static final Map<String, Double> MAX_SCORES_PART_A = new LinkedHashMap<>();

    static {
        // A.I.1 (5 điểm)
        MAX_SCORES_PART_A.put("A_I_1_a", 0.75);
        MAX_SCORES_PART_A.put("A_I_1_b", 0.50);
        MAX_SCORES_PART_A.put("A_I_1_c", 0.50);
        MAX_SCORES_PART_A.put("A_I_1_d", 0.75);
        MAX_SCORES_PART_A.put("A_I_1_dd", 0.75);
        MAX_SCORES_PART_A.put("A_I_1_e", 0.75);
        MAX_SCORES_PART_A.put("A_I_1_ee", 0.50);
        MAX_SCORES_PART_A.put("A_I_1_g", 0.50);

        // A.I.2 (5 điểm)
        MAX_SCORES_PART_A.put("A_I_2_a", 1.00);
        MAX_SCORES_PART_A.put("A_I_2_b", 1.00);
        MAX_SCORES_PART_A.put("A_I_2_c", 1.00);
        MAX_SCORES_PART_A.put("A_I_2_d", 2.00);

        // A.II.1 (2.5 điểm)
        MAX_SCORES_PART_A.put("A_II_1_a", 1.00);
        MAX_SCORES_PART_A.put("A_II_1_b", 0.75);
        MAX_SCORES_PART_A.put("A_II_1_c", 0.75);

        // A.II.2 (2.5 điểm)
        MAX_SCORES_PART_A.put("A_II_2_a", 1.50);
        MAX_SCORES_PART_A.put("A_II_2_b", 1.00);

        // A.II.3 (2.5 điểm)
        MAX_SCORES_PART_A.put("A_II_3_a", 1.50);
        MAX_SCORES_PART_A.put("A_II_3_b", 1.00);

        // A.II.4 (2.5 điểm)
        MAX_SCORES_PART_A.put("A_II_4_a", 1.50);
        MAX_SCORES_PART_A.put("A_II_4_b", 1.00);

        // A.III (10 điểm)
        MAX_SCORES_PART_A.put("A_III_1", 2.50);
        MAX_SCORES_PART_A.put("A_III_2", 2.50);
        MAX_SCORES_PART_A.put("A_III_3", 2.50);
        MAX_SCORES_PART_A.put("A_III_4", 2.50);
    }

    // Các lựa chọn thang điểm phần B
    public static final Map<String, Double> SCORES_B1 = Map.of(
            "a", 20.0,
            "b", 15.0,
            "c", 10.0,
            "d", 5.0,
            "dd", 1.0,
            "e", 0.0
    );

    public static final Map<String, Double> SCORES_B2 = Map.of(
            "a", 20.0,
            "b", 15.0,
            "c", 10.0,
            "d", 5.0,
            "dd", 1.0,
            "e", 0.0
    );

    public static final Map<String, Double> SCORES_B3 = Map.of(
            "a", 30.0,
            "b", 25.0,
            "c", 20.0,
            "d", 15.0,
            "dd", 10.0,
            "e", 5.0,
            "g", 0.0
    );

    // Điểm cộng tối đa từng mục C.I
    public static final Map<String, Double> MAX_SCORES_PART_C1 = Map.of(
            "C_I_1", 1.00,
            "C_I_2", 1.00,
            "C_I_3", 2.00,
            "C_I_4", 2.00
    );

    private static String normalizeOption(String option, String prefix) {
        if (option == null) return null;
        String opt = option.trim();
        if (opt.startsWith(prefix)) {
            opt = opt.substring(prefix.length());
        }
        return opt;
    }

    public static double getScoreB1(String option) {
        String opt = normalizeOption(option, "B_1_");
        return opt != null ? SCORES_B1.getOrDefault(opt, 0.0) : 0.0;
    }

    public static double getScoreB2(String option) {
        String opt = normalizeOption(option, "B_2_");
        return opt != null ? SCORES_B2.getOrDefault(opt, 0.0) : 0.0;
    }

    public static double getScoreB3(String option) {
        String opt = normalizeOption(option, "B_3_");
        return opt != null ? SCORES_B3.getOrDefault(opt, 0.0) : 0.0;
    }

    /**
     * Tự động đề xuất xếp loại theo điểm tổng:
     * - Từ 90 điểm trở lên: Hoàn thành xuất sắc nhiệm vụ
     * - Từ 70 đến dưới 90 điểm: Hoàn thành tốt nhiệm vụ
     * - Từ 50 đến dưới 70 điểm: Hoàn thành nhiệm vụ
     * - Dưới 50 điểm: Không hoàn thành nhiệm vụ
     */
    public static String determineClassification(BigDecimal totalScore) {
        if (totalScore == null) return "Chưa xếp loại";
        double score = totalScore.doubleValue();
        if (score >= 90.0) {
            return "Hoàn thành xuất sắc nhiệm vụ";
        } else if (score >= 70.0) {
            return "Hoàn thành tốt nhiệm vụ";
        } else if (score >= 50.0) {
            return "Hoàn thành nhiệm vụ";
        } else {
            return "Không hoàn thành nhiệm vụ";
        }
    }

    public static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

