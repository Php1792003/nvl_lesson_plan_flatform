package com.school.lessonplan.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action IS NULL OR a.action = :action)
          AND (:actorEmail IS NULL OR a.actorEmailSnapshot LIKE CONCAT('%', :actorEmail, '%'))
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> searchLogs(
        @Param("action") String action,
        @Param("actorEmail") String actorEmail,
        Pageable pageable
    );
}
