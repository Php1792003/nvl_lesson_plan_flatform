package com.school.lessonplan.lessonplan;

import com.school.lessonplan.department.Department;
import com.school.lessonplan.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lesson_plans")
public class LessonPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "owner_snapshot_name", length = 400)
    private String ownerSnapshotName;

    @Column(name = "owner_snapshot_email", length = 320)
    private String ownerSnapshotEmail;

    @Column(name = "owner_snapshot_department", length = 200)
    private String ownerSnapshotDepartment;

    @Column(name = "owner_snapshot_campus", length = 200)
    private String ownerSnapshotCampus;

    @Column(nullable = false, length = 500)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 50)
    private String grade;

    @Column(name = "school_year", nullable = false, length = 20)
    private String schoolYear;

    @Column(nullable = false)
    private int semester;

    @Column(name = "week_no")
    private Integer weekNo;

    @Column(name = "period_no")
    private Integer periodNo;

    @Column(length = 2000)
    private String description;

    @Column(name = "current_version_no", nullable = false)
    private int currentVersionNo = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;

    @Column(name = "favorited_at")
    private Instant favoritedAt;

    public LessonPlan() {}

    // === Getters and Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getOwnerSnapshotName() { return ownerSnapshotName; }
    public void setOwnerSnapshotName(String ownerSnapshotName) { this.ownerSnapshotName = ownerSnapshotName; }

    public String getOwnerSnapshotEmail() { return ownerSnapshotEmail; }
    public void setOwnerSnapshotEmail(String ownerSnapshotEmail) { this.ownerSnapshotEmail = ownerSnapshotEmail; }

    public String getOwnerSnapshotDepartment() { return ownerSnapshotDepartment; }
    public void setOwnerSnapshotDepartment(String ownerSnapshotDepartment) { this.ownerSnapshotDepartment = ownerSnapshotDepartment; }

    public String getOwnerSnapshotCampus() { return ownerSnapshotCampus; }
    public void setOwnerSnapshotCampus(String ownerSnapshotCampus) { this.ownerSnapshotCampus = ownerSnapshotCampus; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public Integer getWeekNo() { return weekNo; }
    public void setWeekNo(Integer weekNo) { this.weekNo = weekNo; }

    public Integer getPeriodNo() { return periodNo; }
    public void setPeriodNo(Integer periodNo) { this.periodNo = periodNo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(int currentVersionNo) { this.currentVersionNo = currentVersionNo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public Instant getFavoritedAt() { return favoritedAt; }
    public void setFavoritedAt(Instant favoritedAt) { this.favoritedAt = favoritedAt; }

    /**
     * Kiểm tra đây có phải giáo án mồ côi (chủ đã bị xoá) không
     */
    public boolean isOrphan() {
        return owner == null;
    }

    /**
     * Lấy tên hiển thị - dùng snapshot nếu là giáo án mồ côi
     */
    public String getDisplayOwnerName() {
        if (owner != null) {
            return owner.getFullName();
        }
        return ownerSnapshotName != null
            ? "Giáo viên đã bị xoá — " + ownerSnapshotName
            : "Giáo viên đã bị xoá";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
