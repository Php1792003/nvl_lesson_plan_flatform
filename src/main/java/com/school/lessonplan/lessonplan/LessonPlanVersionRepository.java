package com.school.lessonplan.lessonplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonPlanVersionRepository extends JpaRepository<LessonPlanVersion, Long> {

    List<LessonPlanVersion> findByPlanIdOrderByVersionNoDesc(Long planId);

    Optional<LessonPlanVersion> findByPlanIdAndVersionNo(Long planId, int versionNo);

    @Query("""
        SELECT v FROM LessonPlanVersion v
        WHERE v.plan.id = :planId
        ORDER BY v.versionNo DESC
    """)
    List<LessonPlanVersion> findAllByPlanId(@Param("planId") Long planId);

    /**
     * Tìm version hiện tại (mới nhất) của giáo án
     */
    @Query("""
        SELECT v FROM LessonPlanVersion v
        WHERE v.plan.id = :planId AND v.versionNo = (
            SELECT lp.currentVersionNo FROM LessonPlan lp WHERE lp.id = :planId
        )
    """)
    Optional<LessonPlanVersion> findCurrentVersion(@Param("planId") Long planId);

    /**
     * Kiểm tra sha256 có trùng version hiện tại không
     */
    @Query("""
        SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
        FROM LessonPlanVersion v
        WHERE v.plan.id = :planId
          AND v.versionNo = (SELECT lp.currentVersionNo FROM LessonPlan lp WHERE lp.id = :planId)
          AND v.sha256 = :sha256
    """)
    boolean existsCurrentVersionWithSha256(@Param("planId") Long planId, @Param("sha256") String sha256);

    /**
     * Tìm versions cần dọn file (purge) — không nằm trong N version mới nhất
     * và không có đánh giá
     */
    @Query(value = """
        SELECT v.* FROM lesson_plan_versions v
        WHERE v.plan_id = :planId
          AND v.file_purged_at IS NULL
          AND v.id NOT IN (
              SELECT TOP (:keepCount) v2.id FROM lesson_plan_versions v2
              WHERE v2.plan_id = :planId
              ORDER BY v2.version_no DESC
          )
          AND v.id NOT IN (
              SELECT DISTINCT e.version_id FROM evaluations e
              WHERE e.plan_id = :planId
          )
    """, nativeQuery = true)
    List<LessonPlanVersion> findPurgeableVersions(@Param("planId") Long planId, @Param("keepCount") int keepCount);

    @Query("SELECT COALESCE(SUM(v.sizeBytes), 0) FROM LessonPlanVersion v WHERE v.filePurgedAt IS NULL")
    long getTotalActiveStorageBytes();

    @Query("SELECT COUNT(v) FROM LessonPlanVersion v WHERE v.filePurgedAt IS NOT NULL")
    long getTotalPurgedVersionsCount();

    /**
     * Tìm các version đã quá hạn scheduled_delete_at, chưa bị purge,
     * dùng cho job tự động xoá file vật lý sau 30 ngày.
     */
    @Query("""
        SELECT v FROM LessonPlanVersion v
        WHERE v.scheduledDeleteAt IS NOT NULL
          AND v.scheduledDeleteAt <= :now
          AND v.filePurgedAt IS NULL
    """)
    List<LessonPlanVersion> findVersionsDueForDeletion(@Param("now") java.time.Instant now);

    /**
     * Tìm version cũ (không phải version hiện tại) chưa lên lịch xoá và chưa bị purge
     */
    @Query("""
        SELECT v FROM LessonPlanVersion v
        WHERE v.plan.id = :planId
          AND v.versionNo <> :currentVersionNo
          AND v.scheduledDeleteAt IS NULL
          AND v.filePurgedAt IS NULL
    """)
    List<LessonPlanVersion> findOldVersionsNotScheduled(
        @Param("planId") Long planId,
        @Param("currentVersionNo") int currentVersionNo
    );
}
