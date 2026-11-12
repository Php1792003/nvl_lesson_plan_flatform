package com.school.lessonplan.nq52.dto;

import com.school.lessonplan.nq52.Nq52Status;

import java.math.BigDecimal;

/**
 * DTO đại diện cho một dòng nhân sự trong Bảng tổng hợp đánh giá xếp loại NQ52
 */
public class Nq52EvaluationSummaryItemDto {

    private int stt;
    private Long userId;
    private String fullName;
    private String birthYear;
    private String positionTitle;
    private Long departmentId;
    private String departmentName;
    private Long campusId;
    private String campusName;
    private Long evaluationId;
    private Nq52Status status;
    private String statusDisplayName;

    // Xếp loại của các cấp
    private String selfClassification;
    private String reviewerClassification;
    private String bghClassification;
    private BigDecimal bghTotalScore;
    private String bghNotes;

    // Chữ ký số Ban Giám Hiệu
    private boolean bghSigned;
    private String bghSignerName;
    private String bghSignedAtFormatted;

    public Nq52EvaluationSummaryItemDto() {}

    public int getStt() { return stt; }
    public void setStt(int stt) { this.stt = stt; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBirthYear() { return birthYear; }
    public void setBirthYear(String birthYear) { this.birthYear = birthYear; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public String getCampusName() { return campusName; }
    public void setCampusName(String campusName) { this.campusName = campusName; }

    public Long getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Long evaluationId) { this.evaluationId = evaluationId; }

    public Nq52Status getStatus() { return status; }
    public void setStatus(Nq52Status status) { this.status = status; }

    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }

    public String getSelfClassification() { return selfClassification; }
    public void setSelfClassification(String selfClassification) { this.selfClassification = selfClassification; }

    public String getReviewerClassification() { return reviewerClassification; }
    public void setReviewerClassification(String reviewerClassification) { this.reviewerClassification = reviewerClassification; }

    public String getBghClassification() { return bghClassification; }
    public void setBghClassification(String bghClassification) { this.bghClassification = bghClassification; }

    public BigDecimal getBghTotalScore() { return bghTotalScore; }
    public void setBghTotalScore(BigDecimal bghTotalScore) { this.bghTotalScore = bghTotalScore; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }

    public boolean isBghSigned() { return bghSigned; }
    public void setBghSigned(boolean bghSigned) { this.bghSigned = bghSigned; }

    public String getBghSignerName() { return bghSignerName; }
    public void setBghSignerName(String bghSignerName) { this.bghSignerName = bghSignerName; }

    public String getBghSignedAtFormatted() { return bghSignedAtFormatted; }
    public void setBghSignedAtFormatted(String bghSignedAtFormatted) { this.bghSignedAtFormatted = bghSignedAtFormatted; }

    // === Helpers xác định mức độ xếp loại cuối cùng của Ban Giám Hiệu ===

    public String getFinalClassification() {
        if (bghClassification != null && !bghClassification.isBlank()) {
            return bghClassification.trim();
        }
        if (reviewerClassification != null && !reviewerClassification.isBlank()) {
            return reviewerClassification.trim();
        }
        if (selfClassification != null && !selfClassification.isBlank()) {
            return selfClassification.trim();
        }
        return "";
    }

    public String getClassificationCategory() {
        return categorize(getFinalClassification());
    }

    public boolean isExcellent() {
        return "EXCELLENT".equals(getClassificationCategory());
    }

    public boolean isGood() {
        return "GOOD".equals(getClassificationCategory());
    }

    public boolean isSatisfactory() {
        return "SATISFACTORY".equals(getClassificationCategory());
    }

    public boolean isUnsatisfactory() {
        return "UNSATISFACTORY".equals(getClassificationCategory());
    }

    public static String categorize(String text) {
        if (text == null || text.isBlank()) return "NONE";
        String lower = text.toLowerCase().trim();
        if (lower.contains("không") || lower.contains("khoong") || lower.contains("khtnv") || lower.contains("unsatisfactory")) {
            return "UNSATISFACTORY";
        }
        if (lower.contains("xuất sắc") || lower.contains("xuat sac") || lower.contains("htxsnv") || lower.contains("excellent")) {
            return "EXCELLENT";
        }
        if (lower.contains("tốt") || lower.contains("tot") || lower.contains("httnv") || lower.contains("good")) {
            return "GOOD";
        }
        if (lower.contains("hoàn thành") || lower.contains("hoan thanh") || lower.contains("htnv") || lower.contains("satisfactory")) {
            return "SATISFACTORY";
        }
        return "NONE";
    }
}

