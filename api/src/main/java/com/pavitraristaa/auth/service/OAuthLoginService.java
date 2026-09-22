package com.pavitraristaa.auth.service;

import com.pavitraristaa.auth.dto.TokenResponse;
import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.DevicePlatform;
import com.pavitraristaa.auth.entity.Role;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.entity.UserRole;
import com.pavitraristaa.auth.repository.RoleRepository;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.repository.UserRoleRepository;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.util.ContactNormalizer;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginService {

    private final SocialIdentityVerifier socialIdentityVerifier;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SessionTokenService sessionTokenService;
    private final AccountStateGuard accountStateGuard;

    public OAuthLoginService(
            SocialIdentityVerifier socialIdentityVerifier,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SessionTokenService sessionTokenService,
            AccountStateGuard accountStateGuard
    ) {
        this.socialIdentityVerifier = socialIdentityVerifier;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionTokenService = sessionTokenService;
        this.accountStateGuard = accountStateGuard;
    }

    @Transactional
    public TokenResponse loginWithGoogle(String idToken, String deviceName, DevicePlatform platform) {
        return completeLogin(socialIdentityVerifier.verifyGoogle(idToken), deviceName, platform);
    }

    @Transactional
    public TokenResponse loginWithApple(String identityToken, String deviceName, DevicePlatform platform) {
        return completeLogin(socialIdentityVerifier.verifyApple(identityToken), deviceName, platform);
    }

    private TokenResponse completeLogin(SocialIdentity identity, String deviceName, DevicePlatform platform) {
        String email = ContactNormalizer.normalizeEmail(identity.email());
        if (email == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Social login requires an email from the identity provider"
            );
        }
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseGet(() -> createSocialUser(email, identity.emailVerified()));
        if (identity.emailVerified() && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
                user.setAccountStatus(AccountStatus.ACTIVE);
            }
        }
        accountStateGuard.assertCanAuthenticate(user);
        user.setLastLoginAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);
        return sessionTokenService.issueTokens(user, deviceName, platform, false);
    }

    private UserAccount createSocialUser(String email, boolean emailVerified) {
        Instant now = Instant.now();
        UserAccount user = new UserAccount();
        user.setUuid(UUID.randomUUID());
        user.setEmail(email);
        user.setEmailVerified(emailVerified);
        user.setMobileVerified(false);
        user.setAccountStatus(emailVerified ? AccountStatus.ACTIVE : AccountStatus.PENDING_VERIFICATION);
        user.setFailedLoginAttempts((short) 0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        user.setVersion(0L);
        UserAccount saved = userAccountRepository.save(user);
        assignDefaultRole(saved);
        return saved;
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
}
