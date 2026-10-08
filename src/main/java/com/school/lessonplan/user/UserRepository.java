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
        WHERE u.id = :id
    """)
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.code = 'ADMIN'")
    long countAdmins();

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.department
        LEFT JOIN FETCH u.campus
        WHERE (:search IS NULL OR
               u.lastName LIKE CONCAT('%', :search, '%') OR
               u.givenName LIKE CONCAT('%', :search, '%') OR
               u.email LIKE CONCAT('%', :search, '%'))
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
