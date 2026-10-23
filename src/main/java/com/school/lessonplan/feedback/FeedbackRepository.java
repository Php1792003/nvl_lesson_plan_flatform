package com.school.lessonplan.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Feedback> findBySenderEmailIgnoreCaseOrderByCreatedAtDesc(String senderEmail);

    long countByStatus(FeedbackStatus status);

    @Query("SELECT f FROM Feedback f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:category IS NULL OR f.category = :category) AND " +
           "(:keyword IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(f.senderName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(f.senderEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(f.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY f.createdAt DESC")
    Page<Feedback> searchFeedbacks(
            @Param("status") FeedbackStatus status,
            @Param("category") FeedbackCategory category,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
