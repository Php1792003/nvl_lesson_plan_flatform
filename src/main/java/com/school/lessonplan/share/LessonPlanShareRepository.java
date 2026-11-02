package com.school.lessonplan.share;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonPlanShareRepository extends JpaRepository<LessonPlanShare, Long> {

    /**
     * Tìm share đang active giữa plan và user
     */
    @Query("""
        SELECT s FROM LessonPlanShare s
        WHERE s.plan.id = :planId
          AND s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
    """)
    Optional<LessonPlanShare> findActiveShare(@Param("planId") Long planId, @Param("userId") Long userId);

    /**
     * Tất cả share active cho một plan
     */
    @Query("""
        SELECT s FROM LessonPlanShare s
        JOIN FETCH s.sharedWithUser
        WHERE s.plan.id = :planId AND s.revokedAt IS NULL
    """)
    List<LessonPlanShare> findActiveSharesByPlanId(@Param("planId") Long planId);

    /**
     * Giáo án được chia sẻ cho user
     */
    @Query(value = """
        SELECT s FROM LessonPlanShare s
        JOIN FETCH s.plan p
        LEFT JOIN FETCH p.owner
        LEFT JOIN FETCH p.department
        WHERE s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
    """,
    countQuery = """
        SELECT COUNT(s) FROM LessonPlanShare s
        WHERE s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
    """)
    Page<LessonPlanShare> findSharedWithUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT DISTINCT d.name FROM LessonPlanShare s
        JOIN s.plan p
        JOIN p.department d
        WHERE s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        ORDER BY d.name
    """)
    List<String> findDistinctDepartmentNamesBySharedUser(@Param("userId") Long userId);

    @Query(value = """
        SELECT s FROM LessonPlanShare s
        JOIN FETCH s.plan p
        LEFT JOIN FETCH p.owner
        LEFT JOIN FETCH p.department d
        WHERE s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """,
    countQuery = """
        SELECT COUNT(s) FROM LessonPlanShare s
        JOIN s.plan p
        LEFT JOIN p.department d
        WHERE s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<LessonPlanShare> searchSharedWithUser(
        @Param("userId") Long userId,
        @Param("dept") String dept,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Xoá share khi user bị xoá (shared_with_user_id = userId)
     */
    @Modifying
    @Query("DELETE FROM LessonPlanShare s WHERE s.sharedWithUser.id = :userId")
    void deleteBySharedWithUserId(@Param("userId") Long userId);

    /**
     * Set granted_by = NULL khi user bị xoá (user đã cấp share)
     */
    @Modifying
    @Query("UPDATE LessonPlanShare s SET s.grantedBy = NULL WHERE s.grantedBy.id = :userId")
    void nullifyGrantedBy(@Param("userId") Long userId);

    /**
     * Kiểm tra user có active share với plan không
     */
    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM LessonPlanShare s
        WHERE s.plan.id = :planId
          AND s.sharedWithUser.id = :userId
          AND s.revokedAt IS NULL
          AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
    """)
    boolean hasActiveShare(@Param("planId") Long planId, @Param("userId") Long userId);
}
