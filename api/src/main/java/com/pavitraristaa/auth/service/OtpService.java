package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.OtpChallengeRepository;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.util.Hashing;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

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
        if (throttled(destination, purpose)) {
            return;
        }
        String code = otpCodeGenerator.generateNumericCode(properties.getOtp().getLength());
        persistChallenge(user, destination, purpose, passwordEncoder.encode(code));
        sendAfterCommit(destination, purpose, code);
    }

    /** Returns null when the request was throttled (nothing was issued or sent). */
    @Transactional
    public String issueRecoveryToken(UserAccount user, String destination, OtpPurpose purpose) {
        if (throttled(destination, purpose)) {
            return null;
        }
        String token = Hashing.randomUrlSafeToken(32);
        persistChallenge(user, destination, purpose, Hashing.sha256Hex(token));
        sendAfterCommit(destination, purpose, token);
        return token;
    }

    /**
     * Sending is a real email or SMS now, so two things matter that did not when it only logged. First, anyone can
     * ask for a code to any address, so without a limit the resend endpoint is a way to flood a stranger's inbox and
     * use up the email provider's daily quota for everyone else. Second, a request inside the cooldown, or past the
     * hourly cap, is dropped silently - the caller still sees the same "sent if valid" answer, so the limit cannot be
     * used to find out which addresses have accounts. The client should show its own countdown.
     */
    private boolean throttled(String destination, OtpPurpose purpose) {
        PavitraProperties.Otp config = properties.getOtp();
        Instant now = Instant.now();
        boolean insideCooldown = otpChallengeRepository
                .findTopByDestinationAndPurposeOrderByCreatedAtDesc(destination, purpose)
                .map(latest -> latest.getCreatedAt().isAfter(now.minus(config.getResendCooldown())))
                .orElse(false);
        boolean overHourlyCap = !insideCooldown
                && otpChallengeRepository.countByDestinationAndPurposeAndCreatedAtAfter(
                        destination, purpose, now.minus(Duration.ofHours(1))) >= config.getMaxPerHour();
        if (insideCooldown || overHourlyCap) {
            log.warn("OTP request throttled for purpose {} ({})", purpose, insideCooldown ? "cooldown" : "hourly limit");
        }
        return insideCooldown || overHourlyCap;
    }

    /**
     * Sent only once the transaction that saved the code has committed: otherwise a registration that later rolls
     * back would still have emailed a code for an account that does not exist, and the HTTP call to the provider
     * would run while the database transaction is held open.
     */
    private void sendAfterCommit(String destination, OtpPurpose purpose, String code) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    otpSender.send(destination, purpose, code);
                }
            });
        } else {
            otpSender.send(destination, purpose, code);
        }
    }

    // noRollbackFor: incrementAttempts()'s write must persist on a wrong code, or the max-attempts throttle
    // never trips. Callers that are themselves @Transactional (AuthService.verifyOtp/loginWithOtp) need the
    // same attribute on their own method too - a nested method's rollback rule doesn't override an outer one.
    @Transactional(noRollbackFor = ApiException.class)
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
