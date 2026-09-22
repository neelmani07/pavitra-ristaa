package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.OtpChallengeRepository;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.util.Hashing;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpSender otpSender;
    private final PavitraProperties properties;

    public OtpService(
            OtpChallengeRepository otpChallengeRepository,
            PasswordEncoder passwordEncoder,
            OtpCodeGenerator otpCodeGenerator,
            OtpSender otpSender,
            PavitraProperties properties
    ) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpSender = otpSender;
        this.properties = properties;
    }

    @Transactional
    public void issueNumericOtp(UserAccount user, String destination, OtpPurpose purpose) {
        String code = otpCodeGenerator.generateNumericCode(properties.getOtp().getLength());
        persistChallenge(user, destination, purpose, passwordEncoder.encode(code));
        otpSender.send(destination, purpose, code);
    }

    @Transactional
    public String issueRecoveryToken(UserAccount user, String destination, OtpPurpose purpose) {
        String token = Hashing.randomUrlSafeToken(32);
        persistChallenge(user, destination, purpose, Hashing.sha256Hex(token));
        otpSender.send(destination, purpose, token);
        return token;
    }

    @Transactional
    public OtpChallenge consumeNumericOtp(String destination, OtpPurpose purpose, String code) {
        OtpChallenge challenge = otpChallengeRepository
                .findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(destination, purpose)
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID, "OTP is invalid"));
        assertUsable(challenge);
        if (!passwordEncoder.matches(code, challenge.getOtpHash())) {
            incrementAttempts(challenge);
            throw new ApiException(ErrorCode.OTP_INVALID, "OTP is invalid");
        }
        challenge.setVerifiedAt(Instant.now());
        return otpChallengeRepository.save(challenge);
    }

    @Transactional
    public OtpChallenge consumeRecoveryToken(String token, OtpPurpose purpose) {
        OtpChallenge challenge = otpChallengeRepository
                .findTopByOtpHashAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(Hashing.sha256Hex(token), purpose)
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID, "Recovery token is invalid"));
        assertUsable(challenge);
        challenge.setVerifiedAt(Instant.now());
        return otpChallengeRepository.save(challenge);
    }

    public OtpPurpose parsePurpose(String raw) {
        try {
            return OtpPurpose.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported OTP purpose", java.util.Map.of("purpose", raw));
        }
    }

    private void persistChallenge(UserAccount user, String destination, OtpPurpose purpose, String hash) {
        Instant now = Instant.now();
        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        challenge.setDestination(destination);
        challenge.setPurpose(purpose);
        challenge.setOtpHash(hash);
        challenge.setAttemptCount((short) 0);
        challenge.setExpiresAt(now.plus(properties.getOtp().getTtl()));
        challenge.setCreatedAt(now);
        otpChallengeRepository.save(challenge);
    }

    private void assertUsable(OtpChallenge challenge) {
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.OTP_EXPIRED, "OTP has expired");
        }
        if (challenge.getAttemptCount() >= properties.getOtp().getMaxAttempts()) {
            throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS, "Too many OTP attempts");
        }
    }

    private void incrementAttempts(OtpChallenge challenge) {
        challenge.setAttemptCount((short) (challenge.getAttemptCount() + 1));
        otpChallengeRepository.save(challenge);
        if (challenge.getAttemptCount() >= properties.getOtp().getMaxAttempts()) {
            throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS, "Too many OTP attempts");
        }
    }
}
