package com.school.lessonplan.nq52.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO tổng hợp toàn bộ kết quả đánh giá xếp loại NQ52 của một tháng/năm dành riêng cho Ban Giám Hiệu.
 */
public class Nq52EvaluationSummaryDto {

    private int month;
    private int year;
    private Long campusId;
    private String campusName;
    private Long departmentId;
    private String departmentName;

    private int totalCount;
    private int reviewedCount;
    private int unreviewedCount;

    private int excellentCount;       // Hoàn thành Xuất sắc nhiệm vụ
    private int goodCount;            // Hoàn thành tốt nhiệm vụ
    private int satisfactoryCount;    // Hoàn thành nhiệm vụ
    private int unsatisfactoryCount;  // Không hoàn thành nhiệm vụ

    private int bghSignedCount;       // Đã được BGH ký số

    private LocalDate exportDate = LocalDate.now();
    private String bghSignerName;
    private String bghSignedAtFormatted;

    private List<Nq52EvaluationSummaryItemDto> items = new ArrayList<>();

    public Nq52EvaluationSummaryDto() {}

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public String getCampusName() { return campusName; }
    public void setCampusName(String campusName) { this.campusName = campusName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getReviewedCount() { return reviewedCount; }
    public void setReviewedCount(int reviewedCount) { this.reviewedCount = reviewedCount; }

    public int getUnreviewedCount() { return unreviewedCount; }
    public void setUnreviewedCount(int unreviewedCount) { this.unreviewedCount = unreviewedCount; }

    public int getExcellentCount() { return excellentCount; }
    public void setExcellentCount(int excellentCount) { this.excellentCount = excellentCount; }

    public int getGoodCount() { return goodCount; }
    public void setGoodCount(int goodCount) { this.goodCount = goodCount; }

    public int getSatisfactoryCount() { return satisfactoryCount; }
    public void setSatisfactoryCount(int satisfactoryCount) { this.satisfactoryCount = satisfactoryCount; }

    public int getUnsatisfactoryCount() { return unsatisfactoryCount; }
    public void setUnsatisfactoryCount(int unsatisfactoryCount) { this.unsatisfactoryCount = unsatisfactoryCount; }

    public int getBghSignedCount() { return bghSignedCount; }
    public void setBghSignedCount(int bghSignedCount) { this.bghSignedCount = bghSignedCount; }

    public LocalDate getExportDate() { return exportDate; }
    public void setExportDate(LocalDate exportDate) { this.exportDate = exportDate; }

    public String getBghSignerName() { return bghSignerName; }
    public void setBghSignerName(String bghSignerName) { this.bghSignerName = bghSignerName; }

    public String getBghSignedAtFormatted() { return bghSignedAtFormatted; }
    public void setBghSignedAtFormatted(String bghSignedAtFormatted) { this.bghSignedAtFormatted = bghSignedAtFormatted; }

    public List<Nq52EvaluationSummaryItemDto> getItems() { return items; }
    public void setItems(List<Nq52EvaluationSummaryItemDto> items) { this.items = items; }

    public double getExcellentPercent() {
        return totalCount > 0 ? (double) excellentCount * 100.0 / totalCount : 0.0;
    }

    public double getGoodPercent() {
        return totalCount > 0 ? (double) goodCount * 100.0 / totalCount : 0.0;
    }

    public double getSatisfactoryPercent() {
        return totalCount > 0 ? (double) satisfactoryCount * 100.0 / totalCount : 0.0;
    }

    public double getUnsatisfactoryPercent() {
        return totalCount > 0 ? (double) unsatisfactoryCount * 100.0 / totalCount : 0.0;
    }
}

