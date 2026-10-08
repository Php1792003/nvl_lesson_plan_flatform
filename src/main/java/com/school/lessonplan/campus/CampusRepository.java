package com.school.lessonplan.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CampusRepository extends JpaRepository<Campus, Long> {
    Optional<Campus> findByIsDefaultTrue();
    Optional<Campus> findByName(String name);
    boolean existsByName(String name);
}
