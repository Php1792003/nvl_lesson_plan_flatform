package com.school.lessonplan.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    @Query("""
                SELECT e FROM EmailVerificationCode e
                WHERE e.user.id = :userId AND e.consumedAt IS NULL
                ORDER BY e.createdAt DESC
            """)
    List<EmailVerificationCode> findActiveByUserId(@Param("userId") Long userId);

    @Query("""
                SELECT COUNT(e) FROM EmailVerificationCode e
                WHERE e.user.id = :userId
                  AND e.createdAt > :since
            """)
    long countRecentByUserId(@Param("userId") Long userId, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM EmailVerificationCode e WHERE e.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
