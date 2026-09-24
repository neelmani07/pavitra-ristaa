package com.pavitraristaa.profile.repository;

import com.pavitraristaa.profile.entity.ProfileVerification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileVerificationRepository extends JpaRepository<ProfileVerification, Long> {

    @EntityGraph(attributePaths = {"profile", "profile.user"})
    Page<ProfileVerification> findByVerificationStatusOrderByIdAsc(String verificationStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"profile", "profile.user"})
    Optional<ProfileVerification> findByIdAndVerificationStatus(Long id, String verificationStatus);
}
