package com.school.lessonplan.lessonplan;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lesson_plan_versions")
public class LessonPlanVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LessonPlan plan;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 100)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 200)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "file_purged_at")
    private Instant filePurgedAt;

    @Column(name = "scheduled_delete_at")
    private Instant scheduledDeleteAt;

    public LessonPlanVersion() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LessonPlan getPlan() { return plan; }
    public void setPlan(LessonPlan plan) { this.plan = plan; }

    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public Instant getFilePurgedAt() { return filePurgedAt; }
    public void setFilePurgedAt(Instant filePurgedAt) { this.filePurgedAt = filePurgedAt; }

    public Instant getScheduledDeleteAt() { return scheduledDeleteAt; }
    public void setScheduledDeleteAt(Instant scheduledDeleteAt) { this.scheduledDeleteAt = scheduledDeleteAt; }

    public boolean isFilePurged() {
        return filePurgedAt != null;
    }

    public boolean isPurged() {
        return filePurgedAt != null;
    }

    /**
     * Version đã được lên lịch xoá tự động (sau 30 ngày khi có version mới)
     */
    public boolean isScheduledForDeletion() {
        return scheduledDeleteAt != null && filePurgedAt == null;
    }

    /**
     * Số ngày còn lại trước khi tự động xoá file vật lý
     */
    public Long getDaysUntilDeletion() {
        if (scheduledDeleteAt == null) {
            return null;
        }
        long days = java.time.Duration.between(Instant.now(), scheduledDeleteAt).toDays();
        return Math.max(0, days);
    }

    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1024.0);
        } else {
            return String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
}
