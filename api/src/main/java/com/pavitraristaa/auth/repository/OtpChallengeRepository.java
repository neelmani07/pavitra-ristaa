package com.pavitraristaa.auth.repository;

import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge> findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String destination,
            OtpPurpose purpose
    );

    Optional<OtpChallenge> findTopByOtpHashAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String otpHash,
            OtpPurpose purpose
    );
}
