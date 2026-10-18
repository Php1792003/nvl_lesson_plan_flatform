package com.school.lessonplan.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);

    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE role_id = :roleId", nativeQuery = true)
    long countAssignedUsers(@Param("roleId") Long roleId);
}

