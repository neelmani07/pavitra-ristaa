package com.pavitraristaa.auth.repository;

import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    /** Newest OTP ever issued here, used or not - the resend cooldown counts from it. */
    Optional<OtpChallenge> findTopByDestinationAndPurposeOrderByCreatedAtDesc(String destination, OtpPurpose purpose);

    long countByDestinationAndPurposeAndCreatedAtAfter(String destination, OtpPurpose purpose, Instant after);

    Optional<OtpChallenge> findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String destination,
            OtpPurpose purpose
    );

    Optional<OtpChallenge> findTopByOtpHashAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String otpHash,
            OtpPurpose purpose
    );
}
