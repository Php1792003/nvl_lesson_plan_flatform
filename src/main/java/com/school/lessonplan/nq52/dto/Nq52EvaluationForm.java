package com.school.lessonplan.nq52.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Nq52EvaluationForm {

    private Long id;
    private int month;
    private int year;
    private String positionTitle = "Giáo viên";
    private String workUnit = "Trường THCS Nguyễn Văn Linh";

    // Phân loại và nhận xét tự đánh giá
    private String strengths;
    private String weaknesses;
    private String selfClassification;

    // Lựa chọn phần B (Mục 1, 2, 3)
    private String selectedB1;
    private String selectedB2;
    private String selectedB3;

    // Điểm tự chấm cho từng tiêu chí phần A và C
    private Map<String, Double> selfScores = new HashMap<>();

    // Giải trình cho từng tiêu chí (nếu có)
    private Map<String, String> explanations = new HashMap<>();

    // Dành cho Tổ trưởng
    private String reviewerSelectedB1;
    private String reviewerSelectedB2;
    private String reviewerSelectedB3;
    private Map<String, Double> reviewerScores = new HashMap<>();
    private String reviewerNotes;
    private String reviewerClassification;

    // Dành cho Ban Giám Hiệu / Hội đồng thi đua, khen thưởng (Cấp 3)
    private String bghSelectedB1;
    private String bghSelectedB2;
    private String bghSelectedB3;
    private Map<String, Double> bghScores = new HashMap<>();
    private String bghNotes;
    private String bghClassification;

    // Hành động submit: "SAVE" (Lưu nháp), "SUBMIT" (Nộp cho Tổ trưởng), "REVIEW" (Tổ trưởng đánh giá), "COUNCIL_REVIEW" (Hội đồng TĐ-KT đánh giá)
    private String action;

    public Nq52EvaluationForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getWorkUnit() { return workUnit; }
    public void setWorkUnit(String workUnit) { this.workUnit = workUnit; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }

    public String getSelfClassification() { return selfClassification; }
    public void setSelfClassification(String selfClassification) { this.selfClassification = selfClassification; }

    public String getSelectedB1() { return selectedB1; }
    public void setSelectedB1(String selectedB1) { this.selectedB1 = selectedB1; }

    public String getSelectedB2() { return selectedB2; }
    public void setSelectedB2(String selectedB2) { this.selectedB2 = selectedB2; }

    public String getSelectedB3() { return selectedB3; }
    public void setSelectedB3(String selectedB3) { this.selectedB3 = selectedB3; }

    public Map<String, Double> getSelfScores() { return selfScores; }
    public void setSelfScores(Map<String, Double> selfScores) { this.selfScores = selfScores; }

    public Map<String, String> getExplanations() { return explanations; }
    public void setExplanations(Map<String, String> explanations) { this.explanations = explanations; }

    public String getReviewerSelectedB1() { return reviewerSelectedB1; }
    public void setReviewerSelectedB1(String reviewerSelectedB1) { this.reviewerSelectedB1 = reviewerSelectedB1; }

    public String getReviewerSelectedB2() { return reviewerSelectedB2; }
    public void setReviewerSelectedB2(String reviewerSelectedB2) { this.reviewerSelectedB2 = reviewerSelectedB2; }

    public String getReviewerSelectedB3() { return reviewerSelectedB3; }
    public void setReviewerSelectedB3(String reviewerSelectedB3) { this.reviewerSelectedB3 = reviewerSelectedB3; }

    public Map<String, Double> getReviewerScores() { return reviewerScores; }
    public void setReviewerScores(Map<String, Double> reviewerScores) { this.reviewerScores = reviewerScores; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public String getReviewerClassification() { return reviewerClassification; }
    public void setReviewerClassification(String reviewerClassification) { this.reviewerClassification = reviewerClassification; }

    public String getBghSelectedB1() { return bghSelectedB1; }
    public void setBghSelectedB1(String bghSelectedB1) { this.bghSelectedB1 = bghSelectedB1; }

    public String getBghSelectedB2() { return bghSelectedB2; }
    public void setBghSelectedB2(String bghSelectedB2) { this.bghSelectedB2 = bghSelectedB2; }

    public String getBghSelectedB3() { return bghSelectedB3; }
    public void setBghSelectedB3(String bghSelectedB3) { this.bghSelectedB3 = bghSelectedB3; }

    public Map<String, Double> getBghScores() { return bghScores; }
    public void setBghScores(Map<String, Double> bghScores) { this.bghScores = bghScores; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }

    public String getBghClassification() { return bghClassification; }
    public void setBghClassification(String bghClassification) { this.bghClassification = bghClassification; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    // ==========================================
    // CÁC HELPER METHOD TÍNH ĐIỂM TIỂU MỤC (SUB-TOTALS)
    // ==========================================

    private double sumScores(Map<String, Double> map, String... keys) {
        if (map == null) return 0.0;
        double sum = 0.0;
        for (String k : keys) {
            Double val = map.get(k);
            if (val != null) sum += val;
        }
        return sum;
    }

    public boolean hasReviewerData() {
        if (reviewerSelectedB1 != null || reviewerSelectedB2 != null || reviewerSelectedB3 != null) return true;
        if (reviewerScores != null && !reviewerScores.isEmpty()) {
            for (Double v : reviewerScores.values()) {
                if (v != null) return true;
            }
        }
        return false;
    }

    // --- TỰ CHẤM (SELF) ---
    public double getSelfScoreAi1() {
        return Math.min(5.0, sumScores(selfScores, "A_I_1_a", "A_I_1_b", "A_I_1_c", "A_I_1_d", "A_I_1_dd", "A_I_1_e", "A_I_1_ee", "A_I_1_g"));
    }

    public double getSelfScoreAi2() {
        return Math.min(5.0, sumScores(selfScores, "A_I_2_a", "A_I_2_b", "A_I_2_c", "A_I_2_d"));
    }

    public double getSelfScoreAi() {
        return Math.min(10.0, getSelfScoreAi1() + getSelfScoreAi2());
    }

    public double getSelfScoreAii1() {
        return Math.min(2.5, sumScores(selfScores, "A_II_1_a", "A_II_1_b", "A_II_1_c"));
    }

    public double getSelfScoreAii2() {
        return Math.min(2.5, sumScores(selfScores, "A_II_2_a", "A_II_2_b"));
    }

    public double getSelfScoreAii3() {
        return Math.min(2.5, sumScores(selfScores, "A_II_3_a", "A_II_3_b"));
    }

    public double getSelfScoreAii4() {
        return Math.min(2.5, sumScores(selfScores, "A_II_4_a", "A_II_4_b"));
    }

    public double getSelfScoreAii() {
        return Math.min(10.0, getSelfScoreAii1() + getSelfScoreAii2() + getSelfScoreAii3() + getSelfScoreAii4());
    }

    public double getSelfScoreAiii() {
        return Math.min(10.0, sumScores(selfScores, "A_III_1", "A_III_2", "A_III_3", "A_III_4"));
    }

    public double getSelfScoreA() {
        return Math.min(30.0, getSelfScoreAi() + getSelfScoreAii() + getSelfScoreAiii());
    }

    public double getSelfScoreB1() {
        return com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB1(selectedB1);
    }

    public double getSelfScoreB2() {
        return com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB2(selectedB2);
    }

    public double getSelfScoreB3() {
        return com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB3(selectedB3);
    }

    public double getSelfScoreB() {
        return Math.min(70.0, getSelfScoreB1() + getSelfScoreB2() + getSelfScoreB3());
    }

    public double getSelfScoreAb() {
        return getSelfScoreA() + getSelfScoreB();
    }

    public double getSelfScoreCi() {
        return Math.min(6.0, sumScores(selfScores, "C_I_1", "C_I_2", "C_I_3", "C_I_4"));
    }

    public double getSelfScoreCii() {
        return sumScores(selfScores, "C_II_1", "C_II_2", "C_II_3");
    }

    public double getSelfScoreC() {
        return getSelfScoreCi() - getSelfScoreCii();
    }

    public double getSelfTotalScore() {
        return Math.max(0.0, getSelfScoreAb() + getSelfScoreCi() - getSelfScoreCii());
    }

    // --- TỔ TRƯỞNG ĐÁNH GIÁ (REVIEWER) ---
    public Double getReviewerScoreAi1() {
        return hasReviewerData() ? Math.min(5.0, sumScores(reviewerScores, "A_I_1_a", "A_I_1_b", "A_I_1_c", "A_I_1_d", "A_I_1_dd", "A_I_1_e", "A_I_1_ee", "A_I_1_g")) : null;
    }

    public Double getReviewerScoreAi2() {
        return hasReviewerData() ? Math.min(5.0, sumScores(reviewerScores, "A_I_2_a", "A_I_2_b", "A_I_2_c", "A_I_2_d")) : null;
    }

    public Double getReviewerScoreAi() {
        if (!hasReviewerData()) return null;
        return Math.min(10.0, getReviewerScoreAi1() + getReviewerScoreAi2());
    }

    public Double getReviewerScoreAii1() {
        return hasReviewerData() ? Math.min(2.5, sumScores(reviewerScores, "A_II_1_a", "A_II_1_b", "A_II_1_c")) : null;
    }

    public Double getReviewerScoreAii2() {
        return hasReviewerData() ? Math.min(2.5, sumScores(reviewerScores, "A_II_2_a", "A_II_2_b")) : null;
    }

    public Double getReviewerScoreAii3() {
        return hasReviewerData() ? Math.min(2.5, sumScores(reviewerScores, "A_II_3_a", "A_II_3_b")) : null;
    }

    public Double getReviewerScoreAii4() {
        return hasReviewerData() ? Math.min(2.5, sumScores(reviewerScores, "A_II_4_a", "A_II_4_b")) : null;
    }

    public Double getReviewerScoreAii() {
        if (!hasReviewerData()) return null;
        return Math.min(10.0, getReviewerScoreAii1() + getReviewerScoreAii2() + getReviewerScoreAii3() + getReviewerScoreAii4());
    }

    public Double getReviewerScoreAiii() {
        return hasReviewerData() ? Math.min(10.0, sumScores(reviewerScores, "A_III_1", "A_III_2", "A_III_3", "A_III_4")) : null;
    }

    public Double getReviewerScoreA() {
        if (!hasReviewerData()) return null;
        return Math.min(30.0, getReviewerScoreAi() + getReviewerScoreAii() + getReviewerScoreAiii());
    }

    public Double getReviewerScoreB1() {
        return hasReviewerData() ? com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB1(reviewerSelectedB1) : null;
    }

    public Double getReviewerScoreB2() {
        return hasReviewerData() ? com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB2(reviewerSelectedB2) : null;
    }

    public Double getReviewerScoreB3() {
        return hasReviewerData() ? com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB3(reviewerSelectedB3) : null;
    }

    public Double getReviewerScoreB() {
        if (!hasReviewerData()) return null;
        return Math.min(70.0, getReviewerScoreB1() + getReviewerScoreB2() + getReviewerScoreB3());
    }

    public Double getReviewerScoreAb() {
        if (!hasReviewerData()) return null;
        return getReviewerScoreA() + getReviewerScoreB();
    }

    public Double getReviewerScoreCi() {
        return hasReviewerData() ? Math.min(6.0, sumScores(reviewerScores, "C_I_1", "C_I_2", "C_I_3", "C_I_4")) : null;
    }

    public Double getReviewerScoreCii() {
        return hasReviewerData() ? sumScores(reviewerScores, "C_II_1", "C_II_2", "C_II_3") : null;
    }

    public Double getReviewerScoreC() {
        if (!hasReviewerData()) return null;
        return getReviewerScoreCi() - getReviewerScoreCii();
    }

    public Double getReviewerTotalScore() {
        if (!hasReviewerData()) return null;
        return Math.max(0.0, getReviewerScoreAb() + getReviewerScoreCi() - getReviewerScoreCii());
    }

    // --- BAN GIÁM HIỆU / HỘI ĐỒNG THI ĐUA, KHEN THƯỞNG (BGH / COUNCIL) ---
    public boolean hasBghData() {
        if (bghSelectedB1 != null || bghSelectedB2 != null || bghSelectedB3 != null) return true;
        if (bghScores != null && !bghScores.isEmpty()) {
            for (Double v : bghScores.values()) {
                if (v != null) return true;
            }
        }
        return false;
    }

    public Double getBghScoreAi1() {
        return hasBghData() ? Math.min(5.0, sumScores(bghScores, "A_I_1_a", "A_I_1_b", "A_I_1_c", "A_I_1_d", "A_I_1_dd", "A_I_1_e", "A_I_1_ee", "A_I_1_g")) : null;
    }

    public Double getBghScoreAi2() {
        return hasBghData() ? Math.min(5.0, sumScores(bghScores, "A_I_2_a", "A_I_2_b", "A_I_2_c", "A_I_2_d")) : null;
    }

    public Double getBghScoreAi() {
        if (!hasBghData()) return null;
        return Math.min(10.0, (getBghScoreAi1() != null ? getBghScoreAi1() : 0.0) + (getBghScoreAi2() != null ? getBghScoreAi2() : 0.0));
    }

    public Double getBghScoreAii1() {
        return hasBghData() ? Math.min(2.5, sumScores(bghScores, "A_II_1_a", "A_II_1_b", "A_II_1_c")) : null;
    }

    public Double getBghScoreAii2() {
        return hasBghData() ? Math.min(2.5, sumScores(bghScores, "A_II_2_a", "A_II_2_b")) : null;
    }

    public Double getBghScoreAii3() {
        return hasBghData() ? Math.min(2.5, sumScores(bghScores, "A_II_3_a", "A_II_3_b")) : null;
    }

    public Double getBghScoreAii4() {
        return hasBghData() ? Math.min(2.5, sumScores(bghScores, "A_II_4_a", "A_II_4_b")) : null;
    }

    public Double getBghScoreAii() {
        if (!hasBghData()) return null;
        return Math.min(10.0, (getBghScoreAii1() != null ? getBghScoreAii1() : 0.0) + (getBghScoreAii2() != null ? getBghScoreAii2() : 0.0) + (getBghScoreAii3() != null ? getBghScoreAii3() : 0.0) + (getBghScoreAii4() != null ? getBghScoreAii4() : 0.0));
    }

    public Double getBghScoreAiii() {
        return hasBghData() ? Math.min(10.0, sumScores(bghScores, "A_III_1", "A_III_2", "A_III_3", "A_III_4")) : null;
    }

    public Double getBghScoreA() {
        if (!hasBghData()) return null;
        return Math.min(30.0, (getBghScoreAi() != null ? getBghScoreAi() : 0.0) + (getBghScoreAii() != null ? getBghScoreAii() : 0.0) + (getBghScoreAiii() != null ? getBghScoreAiii() : 0.0));
    }

    public Double getBghScoreB1() {
        return hasBghData() ? com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB1(bghSelectedB1) : null;
    }

    public Double getBghScoreB2() {
        return hasBghData() ? com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB2(bghSelectedB2) : null;
    }

    public Double getBghScoreB3() {
        return hasBghData() ? com.school.lessonplan.nq52.Nq52CriteriaCatalog.getScoreB3(bghSelectedB3) : null;
    }

    public Double getBghScoreB() {
        if (!hasBghData()) return null;
        return Math.min(70.0, (getBghScoreB1() != null ? getBghScoreB1() : 0.0) + (getBghScoreB2() != null ? getBghScoreB2() : 0.0) + (getBghScoreB3() != null ? getBghScoreB3() : 0.0));
    }

    public Double getBghScoreAb() {
        if (!hasBghData()) return null;
        return (getBghScoreA() != null ? getBghScoreA() : 0.0) + (getBghScoreB() != null ? getBghScoreB() : 0.0);
    }

    public Double getBghScoreCi() {
        return hasBghData() ? Math.min(6.0, sumScores(bghScores, "C_I_1", "C_I_2", "C_I_3", "C_I_4")) : null;
    }

    public Double getBghScoreCii() {
        return hasBghData() ? sumScores(bghScores, "C_II_1", "C_II_2", "C_II_3") : null;
    }

    public Double getBghScoreC() {
        if (!hasBghData()) return null;
        return (getBghScoreCi() != null ? getBghScoreCi() : 0.0) - (getBghScoreCii() != null ? getBghScoreCii() : 0.0);
    }

    public Double getBghTotalScore() {
        if (!hasBghData()) return null;
        return Math.max(0.0, (getBghScoreAb() != null ? getBghScoreAb() : 0.0) + (getBghScoreCi() != null ? getBghScoreCi() : 0.0) - (getBghScoreCii() != null ? getBghScoreCii() : 0.0));
    }
}

