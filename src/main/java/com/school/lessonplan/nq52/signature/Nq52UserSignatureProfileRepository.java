package com.school.lessonplan.nq52.signature;

import com.school.lessonplan.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Nq52UserSignatureProfileRepository extends JpaRepository<Nq52UserSignatureProfile, Long> {

    Optional<Nq52UserSignatureProfile> findByUserId(Long userId);

    Optional<Nq52UserSignatureProfile> findByUser(User user);
}

