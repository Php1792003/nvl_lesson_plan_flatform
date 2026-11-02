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

    @Query("SELECT DISTINCT d.name FROM LessonPlan lp JOIN lp.department d WHERE lp.owner.id = :userId ORDER BY d.name")
    List<String> findDistinctDepartmentNamesByOwnerId(@Param("userId") Long userId);

    @Query(value = """
        SELECT lp FROM LessonPlan lp
        LEFT JOIN FETCH lp.department d
        WHERE lp.owner.id = :userId
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """,
    countQuery = """
        SELECT COUNT(lp) FROM LessonPlan lp
        LEFT JOIN lp.department d
        WHERE lp.owner.id = :userId
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<LessonPlan> searchMyPlans(
        @Param("userId") Long userId,
        @Param("dept") String dept,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Giáo án mồ côi (giáo viên đã bị xoá khỏi hệ thống)
     */
    Page<LessonPlan> findByOwnerIsNull(Pageable pageable);

    @Query("SELECT DISTINCT d.name FROM LessonPlan lp JOIN lp.department d WHERE lp.owner IS NULL ORDER BY d.name")
    List<String> findDistinctOrphanDepartmentNames();

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
    @Query(value = """
        SELECT lp FROM LessonPlan lp
        LEFT JOIN FETCH lp.department d
        LEFT JOIN FETCH lp.owner o
        WHERE (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(o.givenName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(CONCAT(o.lastName, ' ', o.givenName)) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(CONCAT(o.givenName, ' ', o.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(lp.ownerSnapshotName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(lp.ownerSnapshotEmail) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:departmentId IS NULL OR d.id = :departmentId)
          AND (:schoolYear IS NULL OR :schoolYear = '' OR lp.schoolYear = :schoolYear)
          AND (:semester IS NULL OR lp.semester = :semester)
          AND (:favorite IS NULL OR lp.favorite = :favorite)
    """,
    countQuery = """
        SELECT COUNT(lp) FROM LessonPlan lp
        LEFT JOIN lp.department d
        LEFT JOIN lp.owner o
        WHERE (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(o.givenName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(CONCAT(o.lastName, ' ', o.givenName)) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(CONCAT(o.givenName, ' ', o.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(lp.ownerSnapshotName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(lp.ownerSnapshotEmail) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:departmentId IS NULL OR d.id = :departmentId)
          AND (:schoolYear IS NULL OR :schoolYear = '' OR lp.schoolYear = :schoolYear)
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

    @Query("SELECT DISTINCT lp.schoolYear FROM LessonPlan lp WHERE lp.schoolYear IS NOT NULL AND lp.schoolYear <> '' ORDER BY lp.schoolYear DESC")
    List<String> findDistinctSchoolYears();

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

    @Query("""
        SELECT DISTINCT d.name FROM LessonPlan lp
        JOIN lp.department d
        WHERE lp.favorite = true
          AND (lp.owner.id = :userId OR EXISTS (
              SELECT 1 FROM LessonPlanShare s WHERE s.plan.id = lp.id AND s.sharedWithUser.id = :userId AND s.revokedAt IS NULL AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
          ))
        ORDER BY d.name
    """)
    List<String> findDistinctFavoriteDepartmentNames(@Param("userId") Long userId);

    @Query("SELECT DISTINCT d.name FROM LessonPlan lp JOIN lp.department d WHERE lp.favorite = true ORDER BY d.name")
    List<String> findDistinctAllFavoriteDepartmentNames();

    @Query(value = """
        SELECT DISTINCT lp FROM LessonPlan lp
        LEFT JOIN FETCH lp.department d
        LEFT JOIN LessonPlanShare s ON s.plan.id = lp.id
            AND s.sharedWithUser.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        WHERE (lp.owner.id = :userId OR s.id IS NOT NULL)
          AND lp.favorite = true
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """,
    countQuery = """
        SELECT COUNT(DISTINCT lp) FROM LessonPlan lp
        LEFT JOIN lp.department d
        LEFT JOIN LessonPlanShare s ON s.plan.id = lp.id
            AND s.sharedWithUser.id = :userId
            AND s.revokedAt IS NULL
            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
        WHERE (lp.owner.id = :userId OR s.id IS NOT NULL)
          AND lp.favorite = true
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<LessonPlan> searchFavoriteAccessiblePlans(
        @Param("userId") Long userId,
        @Param("dept") String dept,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Danh sách tất cả giáo án yêu thích (dành cho Admin / BGH)
     */
    Page<LessonPlan> findByFavoriteTrue(Pageable pageable);

    @Query(value = """
        SELECT lp FROM LessonPlan lp
        LEFT JOIN FETCH lp.department d
        WHERE lp.favorite = true
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """,
    countQuery = """
        SELECT COUNT(lp) FROM LessonPlan lp
        LEFT JOIN lp.department d
        WHERE lp.favorite = true
          AND (:dept IS NULL OR :dept = '' OR d.name = :dept)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(lp.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<LessonPlan> searchAdminFavoritePlans(
        @Param("dept") String dept,
        @Param("search") String search,
        Pageable pageable
    );

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
