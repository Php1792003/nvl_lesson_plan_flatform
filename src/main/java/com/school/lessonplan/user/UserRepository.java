package com.school.lessonplan.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        LEFT JOIN FETCH u.department
        LEFT JOIN FETCH u.campus
        WHERE u.id = :id
    """)
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.code = 'ADMIN'")
    long countAdmins();

    @Query(value = """
        SELECT u FROM User u
        LEFT JOIN FETCH u.department
        LEFT JOIN FETCH u.campus
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(CONCAT(u.lastName, ' ', u.givenName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(CONCAT(u.givenName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR u.status = :status)
          AND (:departmentId IS NULL OR u.department.id = :departmentId)
          AND (:campusId IS NULL OR u.campus.id = :campusId)
    """,
    countQuery = """
        SELECT COUNT(u) FROM User u
        LEFT JOIN u.department
        LEFT JOIN u.campus
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(CONCAT(u.lastName, ' ', u.givenName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(CONCAT(u.givenName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR u.status = :status)
          AND (:departmentId IS NULL OR u.department.id = :departmentId)
          AND (:campusId IS NULL OR u.campus.id = :campusId)
    """)
    Page<User> searchUsers(
        @Param("search") String search,
        @Param("status") UserStatus status,
        @Param("departmentId") Long departmentId,
        @Param("campusId") Long campusId,
        Pageable pageable
    );
}
