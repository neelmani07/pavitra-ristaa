package com.pavitraristaa.trust.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.profile.entity.ProfileVerification;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.trust.dto.SubmitVerificationRequest;
import com.pavitraristaa.trust.dto.VerificationStatusResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Status tracking only. Evidence storage (government ID / selfie upload) is explicitly deferred in this
 * project's docs beyond profile_verification's own fields, and approving/rejecting a PENDING request is an
 * admin action (POST /admin/verifications/{id}/approve|reject) that doesn't exist yet - so a submitted request
 * stays PENDING until the Admin module adds that. Not a gap unique to this feature: the contract's own status
 * lifecycle already puts approval on the admin side.
 */
@Service
public class VerificationService {

    private static final String UNVERIFIED = "UNVERIFIED";
    private static final String PENDING = "PENDING";

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;

    public VerificationService(AuthService authService, UserProfileRepository userProfileRepository) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public VerificationStatusResponse myStatus(AuthenticatedUser principal) {
        UserProfile profile = requireProfile(principal);
        return toResponse(profile.getVerification());
    }

    @Transactional
    public VerificationStatusResponse submit(AuthenticatedUser principal, SubmitVerificationRequest request) {
        UserProfile profile = requireProfile(principal);
        ProfileVerification verification = profile.getVerification();
        if (verification == null) {
            verification = new ProfileVerification();
            verification.setProfile(profile);
            profile.setVerification(verification);
        }
        verification.setVerificationStatus(PENDING);
        verification.setVerificationType(request.verificationType());
        verification.setVerifiedAt(null);
        verification.setVerifiedBy(null);
        // save() on an already-managed profile merges and returns a different (managed) object graph; the
        // original local `verification` reference is never backfilled with its generated id, only the copy
        // reachable from the returned entity is - read it from there, not from `verification` itself.
        UserProfile saved = userProfileRepository.saveAndFlush(profile);
        return toResponse(saved.getVerification());
    }

    @Transactional(readOnly = true)
    public VerificationStatusResponse getOne(AuthenticatedUser principal, Long verificationId) {
        UserProfile profile = requireProfile(principal);
        ProfileVerification verification = profile.getVerification();
        if (verification == null || !verification.getId().equals(verificationId)) {
            throw new ApiException(ErrorCode.VERIFICATION_NOT_FOUND, "Verification not found");
        }
        return toResponse(verification);
    }

    private UserProfile requireProfile(AuthenticatedUser principal) {
        UserAccount user = authService.requireUsable(principal);
        return userProfileRepository.findByUserAndDeletedFalse(user)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
    }

    private VerificationStatusResponse toResponse(ProfileVerification verification) {
        if (verification == null) {
            return new VerificationStatusResponse(null, UNVERIFIED, null, null, null);
        }
        return new VerificationStatusResponse(
                verification.getId(),
                verification.getVerificationStatus(),
                verification.getVerificationType(),
                verification.getVerifiedAt(),
                verification.getNotes()
        );
    }
}
