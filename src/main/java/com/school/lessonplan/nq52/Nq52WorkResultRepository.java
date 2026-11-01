package com.school.lessonplan.nq52;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Nq52WorkResultRepository extends JpaRepository<Nq52WorkResult, Long> {

    Optional<Nq52WorkResult> findByUserIdAndEvaluationMonthAndEvaluationYear(Long userId, int month, int year);

    List<Nq52WorkResult> findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(Long userId);

    @Query("SELECT r FROM Nq52WorkResult r WHERE r.user.id = :userId " +
           "AND (:year IS NULL OR r.evaluationYear = :year) " +
           "AND (:month IS NULL OR r.evaluationMonth = :month) " +
           "ORDER BY r.evaluationYear DESC, r.evaluationMonth DESC, r.id DESC")
    List<Nq52WorkResult> findMyWorkResultsFiltered(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    @Query("SELECT r FROM Nq52WorkResult r JOIN FETCH r.user u LEFT JOIN FETCH u.campus c LEFT JOIN FETCH r.department d " +
           "WHERE (:campusId IS NULL OR (u.campus IS NOT NULL AND u.campus.id = :campusId)) " +
           "AND (:deptId IS NULL OR d.id = :deptId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:year IS NULL OR r.evaluationYear = :year) " +
           "AND (:month IS NULL OR r.evaluationMonth = :month) " +
           "ORDER BY r.evaluationYear DESC, r.evaluationMonth DESC, r.submittedAt DESC, r.id DESC")
    List<Nq52WorkResult> findDepartmentWorkResultsFiltered(
            @Param("campusId") Long campusId,
            @Param("deptId") Long deptId,
            @Param("status") Nq52Status status,
            @Param("month") Integer month,
            @Param("year") Integer year);

    @Query("SELECT r FROM Nq52WorkResult r JOIN FETCH r.user u LEFT JOIN FETCH u.campus c JOIN FETCH r.department d WHERE r.id = :id")
    Optional<Nq52WorkResult> findByIdWithDetails(@Param("id") Long id);
}

