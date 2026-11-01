package com.pavitraristaa.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.OtpChallengeRepository;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String EMAIL = "ada@example.com";

    @Mock private OtpChallengeRepository otpChallengeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpCodeGenerator otpCodeGenerator;
    @Mock private OtpSender otpSender;

    private OtpService otpService;
    private final UserAccount user = new UserAccount();

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpChallengeRepository, passwordEncoder, otpCodeGenerator, otpSender, new PavitraProperties());
        lenient().when(otpCodeGenerator.generateNumericCode(6)).thenReturn("482913");
        lenient().when(passwordEncoder.encode("482913")).thenReturn("hashed");
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private OtpChallenge issuedAgo(Duration ago) {
        OtpChallenge challenge = new OtpChallenge();
        challenge.setCreatedAt(Instant.now().minus(ago));
        return challenge;
    }

    @Test
    void aSecondRequestInsideTheCooldownIssuesAndSendsNothing() {
        when(otpChallengeRepository.findTopByDestinationAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(issuedAgo(Duration.ofSeconds(10))));

        otpService.issueNumericOtp(user, EMAIL, OtpPurpose.REGISTER);

        verify(otpChallengeRepository, never()).save(any());
        verifyNoInteractions(otpSender);
    }

    @Test
    void pastTheHourlyCapNothingMoreIsIssued() {
        when(otpChallengeRepository.findTopByDestinationAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(issuedAgo(Duration.ofMinutes(5))));
        when(otpChallengeRepository.countByDestinationAndPurposeAndCreatedAtAfter(any(), any(), any())).thenReturn(5L);

        otpService.issueNumericOtp(user, EMAIL, OtpPurpose.REGISTER);

        verify(otpChallengeRepository, never()).save(any());
        verifyNoInteractions(otpSender);
    }

    @Test
    void theRecoveryTokenIsThrottledTheSameWayAndReturnsNull() {
        when(otpChallengeRepository.findTopByDestinationAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.FORGOT_PASSWORD))
                .thenReturn(Optional.of(issuedAgo(Duration.ofSeconds(5))));

        assertThat(otpService.issueRecoveryToken(user, EMAIL, OtpPurpose.FORGOT_PASSWORD)).isNull();
        verifyNoInteractions(otpSender);
    }

    @Test
    void theCodeIsOnlySentOnceTheTransactionHasCommitted() {
        when(otpChallengeRepository.findTopByDestinationAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());
        TransactionSynchronizationManager.initSynchronization();

        otpService.issueNumericOtp(user, EMAIL, OtpPurpose.REGISTER);

        verify(otpChallengeRepository).save(any());
        verifyNoInteractions(otpSender);

        List<TransactionSynchronization> pending = TransactionSynchronizationManager.getSynchronizations();
        pending.forEach(TransactionSynchronization::afterCommit);
        verify(otpSender).send(EMAIL, OtpPurpose.REGISTER, "482913");
    }

    @Test
    void aRolledBackTransactionNeverSendsTheCode() {
        when(otpChallengeRepository.findTopByDestinationAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());
        TransactionSynchronizationManager.initSynchronization();

        otpService.issueNumericOtp(user, EMAIL, OtpPurpose.REGISTER);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(otpSender);
    }

    @Test
    void withNoTransactionAtAllTheCodeIsSentStraightAway() {
        when(otpChallengeRepository.findTopByDestinationAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());

        otpService.issueNumericOtp(user, EMAIL, OtpPurpose.REGISTER);

        verify(otpSender).send(EMAIL, OtpPurpose.REGISTER, "482913");
    }
}
