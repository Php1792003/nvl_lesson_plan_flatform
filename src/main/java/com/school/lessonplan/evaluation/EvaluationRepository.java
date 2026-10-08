package com.school.lessonplan.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByPlanIdOrderByCreatedAtDesc(Long planId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Evaluation e WHERE e.plan.id = :planId")
    boolean existsByPlanId(@Param("planId") Long planId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Evaluation e WHERE e.version.id = :versionId")
    boolean existsByVersionId(@Param("versionId") Long versionId);

    long countByPlanId(Long planId);
}
