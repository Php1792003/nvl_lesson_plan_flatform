package com.school.lessonplan.lessonplan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {

    /**
     * Giáo án của một giáo viên
     */
    Page<LessonPlan> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Giáo án mồ côi (giáo viên đã bị xoá khỏi hệ thống)
     */
    Page<LessonPlan> findByOwnerIsNull(Pageable pageable);

    /**
     * Giáo án mà user được xem (chủ sở hữu HOẶC được chia sẻ)
     */
    @Query(value = """
        SELECT DISTINCT lp FROM LessonPlan lp
        LEFT JOIN LessonPlanShare s ON s.plan.id = lp.id
            AND s.sharedWithUser.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        WHERE lp.owner.id = :userId
           OR s.id IS NOT NULL
    """,
    countQuery = """
        SELECT COUNT(DISTINCT lp) FROM LessonPlan lp
        LEFT JOIN LessonPlanShare s ON s.plan.id = lp.id
            AND s.sharedWithUser.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        WHERE lp.owner.id = :userId
           OR s.id IS NOT NULL
    """)
    Page<LessonPlan> findAccessibleByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Tất cả giáo án (cho BGH) - có filter
     */
    @Query("""
        SELECT lp FROM LessonPlan lp
        LEFT JOIN FETCH lp.department
        LEFT JOIN FETCH lp.owner
        WHERE (:search IS NULL OR lp.title LIKE CONCAT('%', :search, '%'))
          AND (:departmentId IS NULL OR lp.department.id = :departmentId)
          AND (:schoolYear IS NULL OR lp.schoolYear = :schoolYear)
          AND (:semester IS NULL OR lp.semester = :semester)
          AND (:favorite IS NULL OR lp.favorite = :favorite)
    """)
    Page<LessonPlan> searchAll(
        @Param("search") String search,
        @Param("departmentId") Long departmentId,
        @Param("schoolYear") String schoolYear,
        @Param("semester") Integer semester,
        @Param("favorite") Boolean favorite,
        Pageable pageable
    );

    /**
     * Danh sách giáo án yêu thích mà user có quyền truy cập
     */
    @Query(value = """
        SELECT DISTINCT lp FROM LessonPlan lp
        LEFT JOIN LessonPlanShare s ON s.plan.id = lp.id
            AND s.sharedWithUser.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        WHERE (lp.owner.id = :userId OR s.id IS NOT NULL)
          AND lp.favorite = true
    """,
    countQuery = """
        SELECT COUNT(DISTINCT lp) FROM LessonPlan lp
        LEFT JOIN LessonPlanShare s ON s.plan.id = lp.id
            AND s.sharedWithUser.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        WHERE (lp.owner.id = :userId OR s.id IS NOT NULL)
          AND lp.favorite = true
    """)
    Page<LessonPlan> findFavoriteAccessiblePlans(@Param("userId") Long userId, Pageable pageable);

    /**
     * Tìm tất cả giáo án theo năm học và học kỳ (dùng cho BGH dọn dẹp sau học kỳ)
     */
    List<LessonPlan> findBySchoolYearAndSemester(String schoolYear, int semester);

    /**
     * Đếm số giáo án yêu thích trong năm học và học kỳ
     */
    long countBySchoolYearAndSemesterAndFavoriteTrue(String schoolYear, int semester);

    /**
     * Đếm giáo án theo owner
     */
    long countByOwnerId(Long ownerId);

    /**
     * Tìm tất cả giáo án của một user (dùng khi xoá user)
     */
    List<LessonPlan> findAllByOwnerId(Long ownerId);

    /**
     * Đếm tất cả giáo án (bao gồm mồ côi)
     */
    @Query("SELECT COUNT(lp) FROM LessonPlan lp")
    long countAll();

    @Query("SELECT d.name, COUNT(lp) FROM LessonPlan lp JOIN lp.department d GROUP BY d.name")
    List<Object[]> countByDepartment();
}
