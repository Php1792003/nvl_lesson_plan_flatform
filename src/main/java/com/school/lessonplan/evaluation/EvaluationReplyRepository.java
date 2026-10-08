package com.school.lessonplan.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluationReplyRepository extends JpaRepository<EvaluationReply, Long> {

    List<EvaluationReply> findByEvaluationIdOrderByCreatedAtAsc(Long evaluationId);

    /**
     * Set author_id = NULL khi user bị xoá (giữ author_name_snapshot)
     */
    @Modifying
    @Query("UPDATE EvaluationReply r SET r.author = NULL WHERE r.author.id = :userId")
    void nullifyAuthorByUserId(@Param("userId") Long userId);
}
