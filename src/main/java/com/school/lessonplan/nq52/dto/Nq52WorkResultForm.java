package com.school.lessonplan.nq52.dto;

import java.util.ArrayList;
import java.util.List;

public class Nq52WorkResultForm {

    private Long id;
    private int month;
    private int year;
    private String positionTitle = "Giáo viên";
    private String workUnit = "Trường THCS Nguyễn Văn Linh";
    private String evaluationDateStr;
    private String action = "SAVE"; // SAVE, SUBMIT, EXPORT

    private List<Nq52WorkResultTaskItem> tasks = new ArrayList<>();
    private String tasksJson;

    // Bảng thống kê tổng hợp
    private int totalAssigned = 0;
    private int totalCompleted = 0;
    private int totalUncompleted = 0;
    private double completionRate = 0.0;

    private int progressEarly = 0;
    private int progressOnTime = 0;
    private int progressLate = 0;
    private double onTimeRate = 0.0;

    private int qualityStandard = 0;
    private int qualityExceeded = 0;
    private int qualitySubstandard = 0;
    private double qualityRate = 0.0;

    private String summaryNotes;
    private String reviewerNotes;
    private String bghNotes;

    public Nq52WorkResultForm() {}

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

    public String getEvaluationDateStr() { return evaluationDateStr; }
    public void setEvaluationDateStr(String evaluationDateStr) { this.evaluationDateStr = evaluationDateStr; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<Nq52WorkResultTaskItem> getTasks() { return tasks; }
    public void setTasks(List<Nq52WorkResultTaskItem> tasks) { this.tasks = tasks; }

    public String getTasksJson() { return tasksJson; }
    public void setTasksJson(String tasksJson) { this.tasksJson = tasksJson; }

    public int getTotalAssigned() { return totalAssigned; }
    public void setTotalAssigned(int totalAssigned) { this.totalAssigned = totalAssigned; }

    public int getTotalCompleted() { return totalCompleted; }
    public void setTotalCompleted(int totalCompleted) { this.totalCompleted = totalCompleted; }

    public int getTotalUncompleted() { return totalUncompleted; }
    public void setTotalUncompleted(int totalUncompleted) { this.totalUncompleted = totalUncompleted; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public int getProgressEarly() { return progressEarly; }
    public void setProgressEarly(int progressEarly) { this.progressEarly = progressEarly; }

    public int getProgressOnTime() { return progressOnTime; }
    public void setProgressOnTime(int progressOnTime) { this.progressOnTime = progressOnTime; }

    public int getProgressLate() { return progressLate; }
    public void setProgressLate(int progressLate) { this.progressLate = progressLate; }

    public double getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(double onTimeRate) { this.onTimeRate = onTimeRate; }

    public int getQualityStandard() { return qualityStandard; }
    public void setQualityStandard(int qualityStandard) { this.qualityStandard = qualityStandard; }

    public int getQualityExceeded() { return qualityExceeded; }
    public void setQualityExceeded(int qualityExceeded) { this.qualityExceeded = qualityExceeded; }

    public int getQualitySubstandard() { return qualitySubstandard; }
    public void setQualitySubstandard(int qualitySubstandard) { this.qualitySubstandard = qualitySubstandard; }

    public double getQualityRate() { return qualityRate; }
    public void setQualityRate(double qualityRate) { this.qualityRate = qualityRate; }

    public String getSummaryNotes() { return summaryNotes; }
    public void setSummaryNotes(String summaryNotes) { this.summaryNotes = summaryNotes; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }
}

