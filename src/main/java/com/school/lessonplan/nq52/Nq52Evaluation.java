package com.school.lessonplan.nq52;

import com.school.lessonplan.department.Department;
import com.school.lessonplan.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "nq52_evaluations")
public class Nq52Evaluation {

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

    // === Điểm tự chấm của cá nhân ===
    @Column(name = "self_score_a", nullable = false, precision = 5, scale = 2)
    private BigDecimal selfScoreA = BigDecimal.ZERO;

    @Column(name = "self_score_b", nullable = false, precision = 5, scale = 2)
    private BigDecimal selfScoreB = BigDecimal.ZERO;

    @Column(name = "self_score_c_plus", nullable = false, precision = 5, scale = 2)
    private BigDecimal selfScoreCPlus = BigDecimal.ZERO;

    @Column(name = "self_score_c_minus", nullable = false, precision = 5, scale = 2)
    private BigDecimal selfScoreCMinus = BigDecimal.ZERO;

    @Column(name = "self_total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal selfTotalScore = BigDecimal.ZERO;

    @Column(name = "self_classification", length = 100)
    private String selfClassification;

    @Column(name = "strengths", length = 2000)
    private String strengths;

    @Column(name = "weaknesses", length = 2000)
    private String weaknesses;

    // === Điểm đánh giá của Tổ trưởng ===
    @Column(name = "reviewer_score_a", precision = 5, scale = 2)
    private BigDecimal reviewerScoreA;

    @Column(name = "reviewer_score_b", precision = 5, scale = 2)
    private BigDecimal reviewerScoreB;

    @Column(name = "reviewer_score_c_plus", precision = 5, scale = 2)
    private BigDecimal reviewerScoreCPlus;

    @Column(name = "reviewer_score_c_minus", precision = 5, scale = 2)
    private BigDecimal reviewerScoreCMinus;

    @Column(name = "reviewer_total_score", precision = 5, scale = 2)
    private BigDecimal reviewerTotalScore;

    @Column(name = "reviewer_classification", length = 100)
    private String reviewerClassification;

    @Column(name = "reviewer_notes", length = 2000)
    private String reviewerNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewer_name_snapshot", length = 400)
    private String reviewerNameSnapshot;

    // === Điểm đánh giá chính thức của Hội đồng thi đua, khen thưởng (Ban Giám Hiệu) ===
    @Column(name = "bgh_score_a", precision = 5, scale = 2)
    private BigDecimal bghScoreA;

    @Column(name = "bgh_score_b", precision = 5, scale = 2)
    private BigDecimal bghScoreB;

    @Column(name = "bgh_score_c_plus", precision = 5, scale = 2)
    private BigDecimal bghScoreCPlus;

    @Column(name = "bgh_score_c_minus", precision = 5, scale = 2)
    private BigDecimal bghScoreCMinus;

    @Column(name = "bgh_total_score", precision = 5, scale = 2)
    private BigDecimal bghTotalScore;

    @Column(name = "bgh_classification", length = 100)
    private String bghClassification;

    @Column(name = "bgh_notes", length = 2000)
    private String bghNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bgh_reviewer_id")
    private User bghReviewer;

    @Column(name = "bgh_reviewer_name_snapshot", length = 400)
    private String bghReviewerNameSnapshot;

    @Column(name = "bgh_reviewed_at")
    private Instant bghReviewedAt;

    @Lob
    @Column(name = "criteria_data_json", columnDefinition = "NVARCHAR(MAX)")
    private String criteriaDataJson;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // === Thông tin file xuất lưu trong CSDL ===
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

    public Nq52Evaluation() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // === Getters & Setters ===

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

    public BigDecimal getSelfScoreA() { return selfScoreA; }
    public void setSelfScoreA(BigDecimal selfScoreA) { this.selfScoreA = selfScoreA; }

    public BigDecimal getSelfScoreB() { return selfScoreB; }
    public void setSelfScoreB(BigDecimal selfScoreB) { this.selfScoreB = selfScoreB; }

    public BigDecimal getSelfScoreCPlus() { return selfScoreCPlus; }
    public void setSelfScoreCPlus(BigDecimal selfScoreCPlus) { this.selfScoreCPlus = selfScoreCPlus; }

    public BigDecimal getSelfScoreCMinus() { return selfScoreCMinus; }
    public void setSelfScoreCMinus(BigDecimal selfScoreCMinus) { this.selfScoreCMinus = selfScoreCMinus; }

    public BigDecimal getSelfTotalScore() { return selfTotalScore; }
    public void setSelfTotalScore(BigDecimal selfTotalScore) { this.selfTotalScore = selfTotalScore; }

    public String getSelfClassification() { return selfClassification; }
    public void setSelfClassification(String selfClassification) { this.selfClassification = selfClassification; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }

    public BigDecimal getReviewerScoreA() { return reviewerScoreA; }
    public void setReviewerScoreA(BigDecimal reviewerScoreA) { this.reviewerScoreA = reviewerScoreA; }

    public BigDecimal getReviewerScoreB() { return reviewerScoreB; }
    public void setReviewerScoreB(BigDecimal reviewerScoreB) { this.reviewerScoreB = reviewerScoreB; }

    public BigDecimal getReviewerScoreCPlus() { return reviewerScoreCPlus; }
    public void setReviewerScoreCPlus(BigDecimal reviewerScoreCPlus) { this.reviewerScoreCPlus = reviewerScoreCPlus; }

    public BigDecimal getReviewerScoreCMinus() { return reviewerScoreCMinus; }
    public void setReviewerScoreCMinus(BigDecimal reviewerScoreCMinus) { this.reviewerScoreCMinus = reviewerScoreCMinus; }

    public BigDecimal getReviewerTotalScore() { return reviewerTotalScore; }
    public void setReviewerTotalScore(BigDecimal reviewerTotalScore) { this.reviewerTotalScore = reviewerTotalScore; }

    public String getReviewerClassification() { return reviewerClassification; }
    public void setReviewerClassification(String reviewerClassification) { this.reviewerClassification = reviewerClassification; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    public String getReviewerNameSnapshot() { return reviewerNameSnapshot; }
    public void setReviewerNameSnapshot(String reviewerNameSnapshot) { this.reviewerNameSnapshot = reviewerNameSnapshot; }

    public BigDecimal getBghScoreA() { return bghScoreA; }
    public void setBghScoreA(BigDecimal bghScoreA) { this.bghScoreA = bghScoreA; }

    public BigDecimal getBghScoreB() { return bghScoreB; }
    public void setBghScoreB(BigDecimal bghScoreB) { this.bghScoreB = bghScoreB; }

    public BigDecimal getBghScoreCPlus() { return bghScoreCPlus; }
    public void setBghScoreCPlus(BigDecimal bghScoreCPlus) { this.bghScoreCPlus = bghScoreCPlus; }

    public BigDecimal getBghScoreCMinus() { return bghScoreCMinus; }
    public void setBghScoreCMinus(BigDecimal bghScoreCMinus) { this.bghScoreCMinus = bghScoreCMinus; }

    public BigDecimal getBghTotalScore() { return bghTotalScore; }
    public void setBghTotalScore(BigDecimal bghTotalScore) { this.bghTotalScore = bghTotalScore; }

    public String getBghClassification() { return bghClassification; }
    public void setBghClassification(String bghClassification) { this.bghClassification = bghClassification; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }

    public User getBghReviewer() { return bghReviewer; }
    public void setBghReviewer(User bghReviewer) { this.bghReviewer = bghReviewer; }

    public String getBghReviewerNameSnapshot() { return bghReviewerNameSnapshot; }
    public void setBghReviewerNameSnapshot(String bghReviewerNameSnapshot) { this.bghReviewerNameSnapshot = bghReviewerNameSnapshot; }

    public Instant getBghReviewedAt() { return bghReviewedAt; }
    public void setBghReviewedAt(Instant bghReviewedAt) { this.bghReviewedAt = bghReviewedAt; }

    public String getCriteriaDataJson() { return criteriaDataJson; }
    public void setCriteriaDataJson(String criteriaDataJson) { this.criteriaDataJson = criteriaDataJson; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

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
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

