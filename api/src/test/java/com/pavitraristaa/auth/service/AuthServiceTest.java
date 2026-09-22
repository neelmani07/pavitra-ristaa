package com.pavitraristaa.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pavitraristaa.auth.dto.LoginRequest;
import com.pavitraristaa.auth.dto.RegisterRequest;
import com.pavitraristaa.auth.dto.RegisterResponse;
import com.pavitraristaa.auth.dto.TokenResponse;
import com.pavitraristaa.auth.dto.VerifyOtpRequest;
import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.auth.entity.Role;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.mapper.AuthMapper;
import com.pavitraristaa.auth.repository.RoleRepository;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.repository.UserRoleRepository;
import com.pavitraristaa.auth.security.JwtService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.ClientContext;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpService otpService;
    @Mock private SessionTokenService sessionTokenService;
    @Mock private LoginHistoryService loginHistoryService;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userAccountRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder,
                otpService,
                sessionTokenService,
                new AccountStateGuard(),
                loginHistoryService,
                new AuthMapper(),
                jwtService,
                new PavitraProperties()
        );
    }

    @Test
    void registerCreatesPendingAccountAndSendsOtp() {
        when(userAccountRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(11L);
            return user;
        });
        Role role = new Role();
        role.setCode("USER");
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(role));

        RegisterResponse response = authService.register(new RegisterRequest(
                "ada@example.com",
                null,
                "password1",
                null,
                null,
                true,
                true
        ));

        assertThat(response.accountStatus()).isEqualTo("PENDING_VERIFICATION");
        assertThat(response.verificationRequired()).isTrue();
        verify(otpService).issueNumericOtp(any(UserAccount.class), eq("ada@example.com"), eq(OtpPurpose.REGISTER));
        verify(userRoleRepository).save(any());
    }

    @Test
    void registerRejectsMissingTerms() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "ada@example.com", null, "password1", null, null, true, false
        )))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void verifyRegisterOtpActivatesAccount() {
        UserAccount user = activePendingUser();
        when(otpService.parsePurpose("REGISTER")).thenReturn(OtpPurpose.REGISTER);
        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        when(otpService.consumeNumericOtp("ada@example.com", OtpPurpose.REGISTER, "123456")).thenReturn(challenge);
        when(userAccountRepository.findById(11L)).thenReturn(Optional.of(user));
        when(sessionTokenService.rolesOf(user)).thenReturn(List.of("USER"));
        when(userAccountRepository.save(user)).thenReturn(user);

        var response = authService.verifyOtp(new VerifyOtpRequest("ada@example.com", "123456", "REGISTER"));

        assertThat(response.emailVerified()).isTrue();
        assertThat(response.accountStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void loginSucceedsWithPassword() {
        UserAccount user = activePendingUser();
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPasswordHash("hashed");
        when(userAccountRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);
        TokenResponse tokens = new TokenResponse("access", "refresh", Instant.now(), "Bearer");
        when(sessionTokenService.issueTokens(eq(user), eq("Pixel"), any(), eq(false))).thenReturn(tokens);

        TokenResponse result = authService.login(
                new LoginRequest("ada@example.com", "password1", false, "Pixel", "ANDROID"),
                new ClientContext("127.0.0.1", "test")
        );

        assertThat(result.accessToken()).isEqualTo("access");
        verify(loginHistoryService).record(eq(user), any(), eq(true), eq(null), any());
    }

    @Test
    void loginRejectsUnverifiedAccount() {
        UserAccount user = activePendingUser();
        user.setPasswordHash("hashed");
        when(userAccountRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ada@example.com", "password1", false, null, null),
                new ClientContext("127.0.0.1", "test")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    @Test
    void loginLocksAfterRepeatedFailures() {
        UserAccount user = activePendingUser();
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPasswordHash("hashed");
        user.setFailedLoginAttempts((short) 4);
        when(userAccountRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ada@example.com", "wrong", false, null, null),
                new ClientContext("127.0.0.1", "test")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getLockedUntil()).isNotNull();
        assertThat(captor.getValue().getFailedLoginAttempts()).isZero();
    }

    @Test
    void deactivateSetsDeactivatedStatusNotSuspended() {
        UserAccount user = activePendingUser();
        user.setAccountStatus(AccountStatus.ACTIVE);
        when(userAccountRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));

        authService.deactivate(principalFor(user), "taking a break");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus()).isEqualTo(AccountStatus.DEACTIVATED);
    }

    @Test
    void reactivateFlipsDeactivatedAccountBackToActive() {
        UserAccount user = activePendingUser();
        user.setAccountStatus(AccountStatus.DEACTIVATED);
        when(userAccountRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));

        authService.reactivate(principalFor(user));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    /**
     * Regression test for the originally reported bug: deactivate() and reactivate() used to share the SUSPENDED
     * status, so an admin-suspended account could undo the suspension by calling POST /auth/reactivate.
     */
    @Test
    void reactivateCannotLiftAnAdminSuspension() {
        UserAccount user = activePendingUser();
        user.setAccountStatus(AccountStatus.SUSPENDED);
        when(userAccountRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.reactivate(principalFor(user)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void deactivatedAccountCannotLogInWithoutReactivating() {
        UserAccount user = activePendingUser();
        user.setAccountStatus(AccountStatus.DEACTIVATED);
        user.setPasswordHash("hashed");
        when(userAccountRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ada@example.com", "password1", false, null, null),
                new ClientContext("127.0.0.1", "test")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private AuthenticatedUser principalFor(UserAccount user) {
        return new AuthenticatedUser(user.getId(), user.getUuid(), List.of("USER"), 1L);
    }

    private UserAccount activePendingUser() {
        UserAccount user = new UserAccount();
        user.setId(11L);
        user.setUuid(UUID.randomUUID());
        user.setEmail("ada@example.com");
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setFailedLoginAttempts((short) 0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setVersion(0L);
        return user;
    }
}
