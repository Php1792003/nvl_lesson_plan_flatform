package com.school.lessonplan.nq52;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Nq52EvaluationRepository extends JpaRepository<Nq52Evaluation, Long> {

    Optional<Nq52Evaluation> findByUserIdAndEvaluationMonthAndEvaluationYear(Long userId, int month, int year);

    List<Nq52Evaluation> findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(Long userId);

    List<Nq52Evaluation> findByDepartmentIdAndStatusInOrderBySubmittedAtDesc(Long departmentId, List<Nq52Status> statuses);

    List<Nq52Evaluation> findByDepartmentIdOrderByEvaluationYearDescEvaluationMonthDesc(Long departmentId);

    List<Nq52Evaluation> findAllByOrderByEvaluationYearDescEvaluationMonthDesc();

    @Query("SELECT e FROM Nq52Evaluation e WHERE e.user.id = :userId " +
           "AND (:year IS NULL OR e.evaluationYear = :year) " +
           "AND (:month IS NULL OR e.evaluationMonth = :month) " +
           "ORDER BY e.evaluationYear DESC, e.evaluationMonth DESC, e.id DESC")
    List<Nq52Evaluation> findMyEvaluationsFiltered(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    @Query("SELECT e FROM Nq52Evaluation e JOIN FETCH e.user u LEFT JOIN FETCH u.campus c LEFT JOIN FETCH e.department d " +
           "WHERE (:campusId IS NULL OR (u.campus IS NOT NULL AND u.campus.id = :campusId)) " +
           "AND (:deptId IS NULL OR d.id = :deptId) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:year IS NULL OR e.evaluationYear = :year) " +
           "AND (:month IS NULL OR e.evaluationMonth = :month) " +
           "ORDER BY e.evaluationYear DESC, e.evaluationMonth DESC, e.submittedAt DESC, e.id DESC")
    List<Nq52Evaluation> findDepartmentEvaluationsFiltered(
            @Param("campusId") Long campusId,
            @Param("deptId") Long deptId,
            @Param("status") Nq52Status status,
            @Param("month") Integer month,
            @Param("year") Integer year);

    @Query("SELECT e FROM Nq52Evaluation e JOIN FETCH e.user u LEFT JOIN FETCH u.campus c JOIN FETCH e.department d WHERE e.id = :id")
    Optional<Nq52Evaluation> findByIdWithDetails(@Param("id") Long id);
}

