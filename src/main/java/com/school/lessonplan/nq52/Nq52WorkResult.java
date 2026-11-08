package com.school.lessonplan.nq52;

import com.school.lessonplan.department.Department;
import com.school.lessonplan.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "nq52_work_results")
public class Nq52WorkResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "evaluation_month", nullable = false)
    private int evaluationMonth;

    @Column(name = "evaluation_year", nullable = false)
    private int evaluationYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Nq52Status status = Nq52Status.DRAFT;

    @Column(name = "teacher_name_snapshot", nullable = false, length = 400)
    private String teacherNameSnapshot;

    @Column(name = "position_title", nullable = false, length = 200)
    private String positionTitle = "Giáo viên";

    @Column(name = "work_unit", nullable = false, length = 300)
    private String workUnit = "Trường THCS Nguyễn Văn Linh";

    @Column(name = "department_name_snapshot", nullable = false, length = 200)
    private String departmentNameSnapshot;

    @Column(name = "campus_name_snapshot", length = 200)
    private String campusNameSnapshot;

    @Column(name = "evaluation_date_str", length = 100)
    private String evaluationDateStr;

    // === Bảng thống kê tổng số sản phẩm / công việc ===
    @Column(name = "total_assigned", nullable = false)
    private int totalAssigned = 0;

    @Column(name = "total_completed", nullable = false)
    private int totalCompleted = 0;

    @Column(name = "total_uncompleted", nullable = false)
    private int totalUncompleted = 0;

    @Column(name = "completion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionRate = BigDecimal.ZERO;

    @Column(name = "progress_early", nullable = false)
    private int progressEarly = 0;

    @Column(name = "progress_on_time", nullable = false)
    private int progressOnTime = 0;

    @Column(name = "progress_late", nullable = false)
    private int progressLate = 0;

    @Column(name = "on_time_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeRate = BigDecimal.ZERO;

    @Column(name = "quality_standard", nullable = false)
    private int qualityStandard = 0;

    @Column(name = "quality_exceeded", nullable = false)
    private int qualityExceeded = 0;

    @Column(name = "quality_substandard", nullable = false)
    private int qualitySubstandard = 0;

    @Column(name = "quality_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityRate = BigDecimal.ZERO;

    @Column(name = "summary_notes", length = 2000)
    private String summaryNotes;

    @Lob
    @Column(name = "tasks_data_json", columnDefinition = "NVARCHAR(MAX)")
    private String tasksDataJson;

    // === Đánh giá thẩm định của Tổ trưởng chuyên môn ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewer_name_snapshot", length = 400)
    private String reviewerNameSnapshot;

    @Column(name = "reviewer_notes", length = 2000)
    private String reviewerNotes;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // === Đánh giá, phê duyệt chính thức của Ban Giám Hiệu ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bgh_reviewer_id")
    private User bghReviewer;

    @Column(name = "bgh_reviewer_name_snapshot", length = 400)
    private String bghReviewerNameSnapshot;

    @Column(name = "bgh_notes", length = 2000)
    private String bghNotes;

    @Column(name = "bgh_reviewed_at")
    private Instant bghReviewedAt;

    // === Thông tin file Excel lưu trữ ===
    @Column(name = "exported_file_name", length = 500)
    private String exportedFileName;

    @Column(name = "exported_file_path", length = 500)
    private String exportedFilePath;

    @Column(name = "exported_file_size")
    private Long exportedFileSize;

    @Column(name = "exported_file_type", length = 100)
    private String exportedFileType;

    @Column(name = "exported_at")
    private Instant exportedAt;

    @Lob
    @Column(name = "exported_file_data")
    private byte[] exportedFileData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Nq52WorkResult() {}

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public int getEvaluationMonth() { return evaluationMonth; }
    public void setEvaluationMonth(int evaluationMonth) { this.evaluationMonth = evaluationMonth; }

    public int getEvaluationYear() { return evaluationYear; }
    public void setEvaluationYear(int evaluationYear) { this.evaluationYear = evaluationYear; }

    public Nq52Status getStatus() { return status; }
    public void setStatus(Nq52Status status) { this.status = status; }

    public String getTeacherNameSnapshot() { return teacherNameSnapshot; }
    public void setTeacherNameSnapshot(String teacherNameSnapshot) { this.teacherNameSnapshot = teacherNameSnapshot; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getWorkUnit() { return workUnit; }
    public void setWorkUnit(String workUnit) { this.workUnit = workUnit; }

    public String getDepartmentNameSnapshot() { return departmentNameSnapshot; }
    public void setDepartmentNameSnapshot(String departmentNameSnapshot) { this.departmentNameSnapshot = departmentNameSnapshot; }

    public String getCampusNameSnapshot() { return campusNameSnapshot; }
    public void setCampusNameSnapshot(String campusNameSnapshot) { this.campusNameSnapshot = campusNameSnapshot; }

    public String getEvaluationDateStr() { return evaluationDateStr; }
    public void setEvaluationDateStr(String evaluationDateStr) { this.evaluationDateStr = evaluationDateStr; }

    public int getTotalAssigned() { return totalAssigned; }
    public void setTotalAssigned(int totalAssigned) { this.totalAssigned = totalAssigned; }

    public int getTotalCompleted() { return totalCompleted; }
    public void setTotalCompleted(int totalCompleted) { this.totalCompleted = totalCompleted; }

    public int getTotalUncompleted() { return totalUncompleted; }
    public void setTotalUncompleted(int totalUncompleted) { this.totalUncompleted = totalUncompleted; }

    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }

    public int getProgressEarly() { return progressEarly; }
    public void setProgressEarly(int progressEarly) { this.progressEarly = progressEarly; }

    public int getProgressOnTime() { return progressOnTime; }
    public void setProgressOnTime(int progressOnTime) { this.progressOnTime = progressOnTime; }

    public int getProgressLate() { return progressLate; }
    public void setProgressLate(int progressLate) { this.progressLate = progressLate; }

    public BigDecimal getOnTimeRate() { return onTimeRate; }
    public void setOnTimeRate(BigDecimal onTimeRate) { this.onTimeRate = onTimeRate; }

    public int getQualityStandard() { return qualityStandard; }
    public void setQualityStandard(int qualityStandard) { this.qualityStandard = qualityStandard; }

    public int getQualityExceeded() { return qualityExceeded; }
    public void setQualityExceeded(int qualityExceeded) { this.qualityExceeded = qualityExceeded; }

    public int getQualitySubstandard() { return qualitySubstandard; }
    public void setQualitySubstandard(int qualitySubstandard) { this.qualitySubstandard = qualitySubstandard; }

    public BigDecimal getQualityRate() { return qualityRate; }
    public void setQualityRate(BigDecimal qualityRate) { this.qualityRate = qualityRate; }

    public String getSummaryNotes() { return summaryNotes; }
    public void setSummaryNotes(String summaryNotes) { this.summaryNotes = summaryNotes; }

    public String getTasksDataJson() { return tasksDataJson; }
    public void setTasksDataJson(String tasksDataJson) { this.tasksDataJson = tasksDataJson; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    public String getReviewerNameSnapshot() { return reviewerNameSnapshot; }
    public void setReviewerNameSnapshot(String reviewerNameSnapshot) { this.reviewerNameSnapshot = reviewerNameSnapshot; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public User getBghReviewer() { return bghReviewer; }
    public void setBghReviewer(User bghReviewer) { this.bghReviewer = bghReviewer; }

    public String getBghReviewerNameSnapshot() { return bghReviewerNameSnapshot; }
    public void setBghReviewerNameSnapshot(String bghReviewerNameSnapshot) { this.bghReviewerNameSnapshot = bghReviewerNameSnapshot; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }

    public Instant getBghReviewedAt() { return bghReviewedAt; }
    public void setBghReviewedAt(Instant bghReviewedAt) { this.bghReviewedAt = bghReviewedAt; }

    public String getExportedFileName() { return exportedFileName; }
    public void setExportedFileName(String exportedFileName) { this.exportedFileName = exportedFileName; }

    public String getExportedFilePath() { return exportedFilePath; }
    public void setExportedFilePath(String exportedFilePath) { this.exportedFilePath = exportedFilePath; }

    public Long getExportedFileSize() { return exportedFileSize; }
    public void setExportedFileSize(Long exportedFileSize) { this.exportedFileSize = exportedFileSize; }

    public String getExportedFileType() { return exportedFileType; }
    public void setExportedFileType(String exportedFileType) { this.exportedFileType = exportedFileType; }

    public Instant getExportedAt() { return exportedAt; }
    public void setExportedAt(Instant exportedAt) { this.exportedAt = exportedAt; }

    public byte[] getExportedFileData() { return exportedFileData; }
    public void setExportedFileData(byte[] exportedFileData) { this.exportedFileData = exportedFileData; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

