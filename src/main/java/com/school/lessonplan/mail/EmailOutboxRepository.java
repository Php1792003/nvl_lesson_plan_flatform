package com.school.lessonplan.mail;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.status = 'PENDING'
          AND e.nextAttemptAt <= :now
        ORDER BY e.nextAttemptAt ASC
    """)
    List<EmailOutbox> findPendingEmails(@Param("now") Instant now, Pageable pageable);

    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.status = 'PENDING'
          AND e.nextAttemptAt <= :now
        ORDER BY e.nextAttemptAt ASC
    """)
    List<EmailOutbox> findPendingEmails(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM EmailOutbox e WHERE e.toEmail = :email AND e.status = 'PENDING'")
    void deletePendingByEmail(@Param("email") String email);
}
