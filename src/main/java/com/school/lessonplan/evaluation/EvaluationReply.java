package com.school.lessonplan.evaluation;

import com.school.lessonplan.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evaluation_replies")
public class EvaluationReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "author_name_snapshot", nullable = false, length = 400)
    private String authorNameSnapshot;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EvaluationReply() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getAuthorNameSnapshot() { return authorNameSnapshot; }
    public void setAuthorNameSnapshot(String authorNameSnapshot) { this.authorNameSnapshot = authorNameSnapshot; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getCreatedAt() { return createdAt; }
}
