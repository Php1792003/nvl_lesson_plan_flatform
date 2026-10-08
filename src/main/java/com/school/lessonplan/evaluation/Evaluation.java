package com.school.lessonplan.evaluation;

import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import com.school.lessonplan.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private LessonPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private LessonPlanVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewer_name_snapshot", nullable = false, length = 400)
    private String reviewerNameSnapshot;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "edited_at")
    private Instant editedAt;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private java.util.List<EvaluationReply> replies = new java.util.ArrayList<>();

    public Evaluation() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LessonPlan getPlan() { return plan; }
    public void setPlan(LessonPlan plan) { this.plan = plan; }

    public LessonPlanVersion getVersion() { return version; }
    public void setVersion(LessonPlanVersion version) { this.version = version; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    public String getReviewerNameSnapshot() { return reviewerNameSnapshot; }
    public void setReviewerNameSnapshot(String reviewerNameSnapshot) { this.reviewerNameSnapshot = reviewerNameSnapshot; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    public java.util.List<EvaluationReply> getReplies() { return replies; }
    public void setReplies(java.util.List<EvaluationReply> replies) { this.replies = replies; }

    public boolean isEdited() { return editedAt != null; }
}
