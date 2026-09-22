package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.dto.AccountResponse;
import com.pavitraristaa.auth.dto.ChangePasswordRequest;
import com.pavitraristaa.auth.dto.DeleteAccountRequest;
import com.pavitraristaa.auth.dto.ForgotPasswordRequest;
import com.pavitraristaa.auth.dto.LinkAccountRequest;
import com.pavitraristaa.auth.dto.LinkedAccountsResponse;
import com.pavitraristaa.auth.dto.LoginOtpRequest;
import com.pavitraristaa.auth.dto.LoginRequest;
import com.pavitraristaa.auth.dto.RegisterRequest;
import com.pavitraristaa.auth.dto.RegisterResponse;
import com.pavitraristaa.auth.dto.ResendOtpRequest;
import com.pavitraristaa.auth.dto.ResetPasswordRequest;
import com.pavitraristaa.auth.dto.SessionResponse;
import com.pavitraristaa.auth.dto.TokenResponse;
import com.pavitraristaa.auth.dto.VerifyEmailRequest;
import com.pavitraristaa.auth.dto.VerifyOtpRequest;
import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.DevicePlatform;
import com.pavitraristaa.auth.entity.LoginType;
import com.pavitraristaa.auth.entity.OtpChallenge;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.auth.entity.Role;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.entity.UserRole;
import com.pavitraristaa.auth.mapper.AuthMapper;
import com.pavitraristaa.auth.repository.RoleRepository;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.repository.UserRoleRepository;
import com.pavitraristaa.auth.security.JwtService;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.ClientContext;
import com.pavitraristaa.common.util.ContactNormalizer;
import com.pavitraristaa.config.PavitraProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DELETE_CONFIRMATION = "DELETE";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final SessionTokenService sessionTokenService;
    private final AccountStateGuard accountStateGuard;
    private final LoginHistoryService loginHistoryService;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final PavitraProperties properties;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            SessionTokenService sessionTokenService,
            AccountStateGuard accountStateGuard,
            LoginHistoryService loginHistoryService,
            AuthMapper authMapper,
            JwtService jwtService,
            PavitraProperties properties
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.sessionTokenService = sessionTokenService;
        this.accountStateGuard = accountStateGuard;
        this.loginHistoryService = loginHistoryService;
        this.authMapper = authMapper;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.acceptedTerms()) || !Boolean.TRUE.equals(request.acceptedPrivacyPolicy())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Terms and privacy policy must be accepted");
        }
        String email = ContactNormalizer.normalizeEmail(request.email());
        String mobile = ContactNormalizer.normalizeMobile(request.mobile());
        if (email == null && mobile == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Email or mobile is required");
        }
        if (email != null && userAccountRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "An account with this email already exists");
        }
        if (mobile != null && userAccountRepository.existsByMobile(mobile)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "An account with this mobile already exists");
        }
        Instant now = Instant.now();
        UserAccount user = new UserAccount();
        user.setUuid(UUID.randomUUID());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user.setMobileVerified(false);
        user.setFailedLoginAttempts((short) 0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        user.setVersion(0L);
        UserAccount saved = userAccountRepository.save(user);
        assignDefaultRole(saved);
        if (email != null) {
            otpService.issueNumericOtp(saved, email, OtpPurpose.REGISTER);
        }
        if (mobile != null) {
            otpService.issueNumericOtp(saved, mobile, OtpPurpose.REGISTER);
        }
        return authMapper.toRegisterResponse(saved);
    }

    // noRollbackFor: otpService.consumeNumericOtp()'s attempt-count increment must persist on a wrong code, or the
    // max-attempts throttle never trips (each wrong guess would roll its own counter increment back to zero).
    @Transactional(noRollbackFor = ApiException.class)
    public AccountResponse verifyOtp(VerifyOtpRequest request) {
        OtpPurpose purpose = otpService.parsePurpose(request.purpose());
        String destination = normalizeDestination(request.destination());
        OtpChallenge challenge = otpService.consumeNumericOtp(destination, purpose, request.otp());
        UserAccount user = resolveUser(challenge.getUser(), destination);
        if (purpose == OtpPurpose.REGISTER || purpose == OtpPurpose.CHANGE_EMAIL || purpose == OtpPurpose.CHANGE_MOBILE) {
            markContactVerified(user, destination);
            if (user.isEmailVerified() || user.isMobileVerified()) {
                if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
                    user.setAccountStatus(AccountStatus.ACTIVE);
                }
            }
            user.setUpdatedAt(Instant.now());
            userAccountRepository.save(user);
        }
        return authMapper.toAccountResponse(user, sessionTokenService.rolesOf(user));
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        OtpPurpose purpose = otpService.parsePurpose(request.purpose());
        String destination = normalizeDestination(request.destination());
        UserAccount user = findByDestination(destination).orElse(null);
        if (user == null) {
            return;
        }
        otpService.issueNumericOtp(user, destination, purpose);
    }

    @Transactional
    public AccountResponse verifyEmail(VerifyEmailRequest request) {
        UUID userUuid = jwtService.parseEmailVerificationToken(request.token());
        UserAccount user = userAccountRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        user.setEmailVerified(true);
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        return authMapper.toAccountResponse(user, sessionTokenService.rolesOf(user));
    }

    // noRollbackFor: registerFailedLogin()'s attempt count / lockout write must persist even though this method
    // throws on bad credentials - otherwise the brute-force lockout can never trigger (every failed attempt
    // rolls its own counter increment back to what it was before).
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(LoginRequest request, ClientContext context) {
        String identifier = request.identifier().contains("@")
                ? ContactNormalizer.normalizeEmail(request.identifier())
                : ContactNormalizer.normalizeMobile(request.identifier());
        UserAccount user = identifier != null && identifier.contains("@")
                ? userAccountRepository.findByEmail(identifier).orElse(null)
                : userAccountRepository.findByMobile(identifier).orElse(null);
        if (user == null) {
            loginHistoryService.record(null, LoginType.PASSWORD, false, "UNKNOWN_ACCOUNT", context);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            loginHistoryService.record(user, LoginType.PASSWORD, false, "LOCKED", context);
            throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS, "Account is temporarily locked");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedLogin(user);
            loginHistoryService.record(user, LoginType.PASSWORD, false, "INVALID_PASSWORD", context);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        try {
            accountStateGuard.assertCanAuthenticate(user);
        } catch (ApiException exception) {
            loginHistoryService.record(user, LoginType.PASSWORD, false, exception.getErrorCode().name(), context);
            throw exception;
        }
        clearLock(user);
        user.setLastLoginAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        loginHistoryService.record(user, LoginType.PASSWORD, true, null, context);
        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        return sessionTokenService.issueTokens(
                user,
                request.deviceName(),
                DevicePlatformParser.parse(request.platform()),
                rememberMe
        );
    }

    // noRollbackFor: same reason as verifyOtp() - the OTP attempt-count increment must survive a thrown ApiException.
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse loginWithOtp(LoginOtpRequest request, ClientContext context) {
        String mobile = ContactNormalizer.normalizeMobile(request.mobile());
        UserAccount user = userAccountRepository.findByMobile(mobile).orElse(null);
        if (user == null) {
            loginHistoryService.record(null, LoginType.OTP, false, "UNKNOWN_ACCOUNT", context);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        try {
            otpService.consumeNumericOtp(mobile, OtpPurpose.LOGIN, request.otp());
            accountStateGuard.assertCanAuthenticate(user);
        } catch (ApiException exception) {
            loginHistoryService.record(user, LoginType.OTP, false, exception.getErrorCode().name(), context);
            throw exception;
        }
        if (!user.isMobileVerified()) {
            user.setMobileVerified(true);
        }
        clearLock(user);
        user.setLastLoginAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        loginHistoryService.record(user, LoginType.OTP, true, null, context);
        return sessionTokenService.issueTokens(
                user,
                request.deviceName(),
                DevicePlatformParser.parse(request.platform()),
                false
        );
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        return sessionTokenService.rotate(refreshToken);
    }

    @Transactional
    public void logout(AuthenticatedUser principal) {
        UserAccount user = requireUser(principal);
        if (principal.sessionId() == null) {
            throw new ApiException(ErrorCode.SESSION_REVOKED, "Session not found");
        }
        sessionTokenService.revokeById(user, principal.sessionId());
    }

    @Transactional
    public void logoutAll(AuthenticatedUser principal) {
        sessionTokenService.revokeAll(requireUsable(principal));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.identifier().contains("@")
                ? ContactNormalizer.normalizeEmail(request.identifier())
                : ContactNormalizer.normalizeMobile(request.identifier());
        findByDestination(identifier).ifPresent(user ->
                otpService.issueRecoveryToken(user, identifier, OtpPurpose.FORGOT_PASSWORD));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpChallenge challenge = otpService.consumeRecoveryToken(request.token(), OtpPurpose.FORGOT_PASSWORD);
        UserAccount user = resolveUser(challenge.getUser(), challenge.getDestination());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        clearLock(user);
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        sessionTokenService.revokeAll(user);
    }

    @Transactional
    public void changePassword(AuthenticatedUser principal, ChangePasswordRequest request) {
        UserAccount user = requireUsable(principal);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        sessionTokenService.revokeAllExcept(user, principal.sessionId());
    }

    @Transactional(readOnly = true)
    public PagedData<SessionResponse> listSessions(AuthenticatedUser principal, Pageable pageable) {
        UserAccount user = requireUsable(principal);
        return sessionTokenService.listSessions(user, principal.sessionId(), pageable);
    }

    @Transactional
    public void revokeSession(AuthenticatedUser principal, Long sessionId) {
        sessionTokenService.revokeById(requireUsable(principal), sessionId);
    }

    @Transactional(readOnly = true)
    public AccountResponse me(AuthenticatedUser principal) {
        UserAccount user = requireUsable(principal);
        return authMapper.toAccountResponse(user, sessionTokenService.rolesOf(user));
    }

    @Transactional(readOnly = true)
    public LinkedAccountsResponse linkedAccounts(AuthenticatedUser principal) {
        UserAccount user = requireUsable(principal);
        List<String> linked = new ArrayList<>();
        if (user.getPasswordHash() != null) {
            linked.add("PASSWORD");
        }
        if (user.getEmail() != null) {
            linked.add("EMAIL");
        }
        if (user.getMobile() != null) {
            linked.add("MOBILE");
        }
        return new LinkedAccountsResponse(linked);
    }

    public void linkAccount(AuthenticatedUser principal, String provider, LinkAccountRequest request) {
        requireUsable(principal);
        throw linkedAccountUnsupported(provider);
    }

    public void unlinkAccount(AuthenticatedUser principal, String provider) {
        requireUsable(principal);
        throw linkedAccountUnsupported(provider);
    }

    @Transactional
    public void deactivate(AuthenticatedUser principal, String reason) {
        UserAccount user = requireUsable(principal);
        user.setAccountStatus(AccountStatus.DEACTIVATED);
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        // Sessions are kept (unlike delete/reset), so the same login can call reactivate without a fresh login,
        // which is blocked while DEACTIVATED. A refresh token that also expires before reactivation is used
        // leaves the account reachable only through support - a known limitation, not solved here.
    }

    /**
     * Self-service only: reverses {@link #deactivate}. An admin-suspended account is not reachable through this
     * endpoint - {@code SUSPENDED} requires a moderator/admin action once that capability exists.
     */
    @Transactional
    public void reactivate(AuthenticatedUser principal) {
        UserAccount user = requireUser(principal);
        if (user.getAccountStatus() == AccountStatus.DELETED || user.isDeleted()) {
            throw new ApiException(ErrorCode.ACCOUNT_DELETED, "Account has been deleted");
        }
        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Account is blocked");
        }
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED, "Account is suspended and cannot be self-reactivated");
        }
        if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setUpdatedAt(Instant.now());
            userAccountRepository.save(user);
        }
    }

    @Transactional
    public void deleteAccount(AuthenticatedUser principal, DeleteAccountRequest request) {
        if (!DELETE_CONFIRMATION.equalsIgnoreCase(request.confirmation())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Confirmation must be DELETE",
                    Map.of("confirmation", request.confirmation())
            );
        }
        UserAccount user = requireUsable(principal);
        Instant now = Instant.now();
        user.setAccountStatus(AccountStatus.DELETED);
        user.setDeleted(true);
        user.setDeletedAt(now);
        user.setUpdatedAt(now);
        userAccountRepository.save(user);
        sessionTokenService.revokeAll(user);
    }

    public Map<String, String> requestDataExport(AuthenticatedUser principal) {
        requireUsable(principal);
        return Map.of(
                "status", "ACCEPTED",
                "message", "Data export job tracking is planned for a later version; the request was accepted"
        );
    }

    public UserAccount requireUsable(AuthenticatedUser principal) {
        UserAccount user = requireUser(principal);
        accountStateGuard.assertUsableSession(user);
        return user;
    }

    private UserAccount requireUser(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private void assignDefaultRole(UserAccount user) {
        Role role = roleRepository.findByCode("USER")
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "Default USER role is missing"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(Instant.now());
        userRoleRepository.save(userRole);
    }

    private void markContactVerified(UserAccount user, String destination) {
        if (destination.contains("@")) {
            user.setEmailVerified(true);
            if (user.getEmail() == null) {
                user.setEmail(destination);
            }
        } else {
            user.setMobileVerified(true);
            if (user.getMobile() == null) {
                user.setMobile(destination);
            }
        }
    }

    private UserAccount resolveUser(UserAccount fromChallenge, String destination) {
        if (fromChallenge != null && fromChallenge.getId() != null) {
            return userAccountRepository.findById(fromChallenge.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        }
        return findByDestination(destination)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
    }

    private java.util.Optional<UserAccount> findByDestination(String destination) {
        if (destination == null) {
            return java.util.Optional.empty();
        }
        if (destination.contains("@")) {
            return userAccountRepository.findByEmail(destination);
        }
        return userAccountRepository.findByMobile(destination);
    }

    private String normalizeDestination(String destination) {
        if (destination.contains("@")) {
            return ContactNormalizer.normalizeEmail(destination);
        }
        return ContactNormalizer.normalizeMobile(destination);
    }

    private void registerFailedLogin(UserAccount user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts((short) attempts);
        if (attempts >= properties.getAuth().getMaxFailedLogins()) {
            user.setLockedUntil(Instant.now().plus(properties.getAuth().getLockDuration()));
            user.setFailedLoginAttempts((short) 0);
        }
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
    }

    private void clearLock(UserAccount user) {
        user.setFailedLoginAttempts((short) 0);
        user.setLockedUntil(null);
    }

    private ApiException linkedAccountUnsupported(String provider) {
        return new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "Linking Google or Apple to an existing password account is planned for a later version",
                Map.of("provider", provider == null ? "" : provider.toUpperCase())
        );
    }
}
