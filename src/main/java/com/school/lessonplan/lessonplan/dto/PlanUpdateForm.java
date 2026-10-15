package com.school.lessonplan.lessonplan.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({PlanUpdateForm.class, FormatGroup.class})
public class PlanUpdateForm {

    @NotBlank
    @Size(max = 200, groups = FormatGroup.class)
    private String title;

    @NotNull
    private Long departmentId;

    @NotBlank
    private String grade;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{4}$", groups = FormatGroup.class)
    private String schoolYear;

    @NotNull
    private Integer semester;

    @Min(value = 1, message = "Tuần học từ 1 trở lên.")
    @Max(value = 35, message = "Tuần học tối đa là 35.")
    private Integer weekNo;

    @Min(value = 1, message = "Từ tiết phải từ 1 trở lên.")
    @Max(value = 200, message = "Tiết học không hợp lệ.")
    private Integer startPeriod;

    @Min(value = 1, message = "Đến tiết phải từ 1 trở lên.")
    @Max(value = 200, message = "Tiết học không hợp lệ.")
    private Integer endPeriod;

    @Size(max = 1000, groups = FormatGroup.class)
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade == null ? null : grade.trim();
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear == null ? null : schoolYear.trim();
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getWeekNo() {
        return weekNo;
    }

    public void setWeekNo(Integer weekNo) {
        this.weekNo = weekNo;
    }

    public Integer getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(Integer startPeriod) {
        this.startPeriod = startPeriod;
    }

    public Integer getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(Integer endPeriod) {
        this.endPeriod = endPeriod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}
