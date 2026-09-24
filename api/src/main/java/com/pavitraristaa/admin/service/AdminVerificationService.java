package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminVerificationResponse;
import com.pavitraristaa.admin.dto.VerificationDecisionRequest;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.profile.entity.ProfileVerification;
import com.pavitraristaa.profile.repository.ProfileVerificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Approve/reject side of trust.VerificationService.submit() - the admin half of the profile_verification lifecycle. */
@Service
public class AdminVerificationService {

    private static final String PENDING = "PENDING";
    private static final String VERIFIED = "VERIFIED";
    private static final String REJECTED = "REJECTED";

    private final ProfileVerificationRepository profileVerificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public AdminVerificationService(
            ProfileVerificationRepository profileVerificationRepository,
            UserAccountRepository userAccountRepository,
            AuditLogService auditLogService
    ) {
        this.profileVerificationRepository = profileVerificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminVerificationResponse> listPending(Integer page, Integer size) {
        Page<ProfileVerification> result = profileVerificationRepository.findByVerificationStatusOrderByIdAsc(
                PENDING, PaginationSupport.pageable(page, size));
        List<AdminVerificationResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public AdminVerificationResponse approve(AuthenticatedUser principal, Long verificationId, VerificationDecisionRequest request) {
        return decide(principal, verificationId, request, VERIFIED, "VERIFICATION_APPROVED");
    }

    @Transactional
    public AdminVerificationResponse reject(AuthenticatedUser principal, Long verificationId, VerificationDecisionRequest request) {
        return decide(principal, verificationId, request, REJECTED, "VERIFICATION_REJECTED");
    }

    private AdminVerificationResponse decide(
            AuthenticatedUser principal, Long verificationId, VerificationDecisionRequest request, String newStatus, String auditAction) {
        ProfileVerification verification = profileVerificationRepository.findByIdAndVerificationStatus(verificationId, PENDING)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFICATION_NOT_FOUND, "No pending verification with that id"));
        UserAccount admin = actingAdmin(principal);
        verification.setVerificationStatus(newStatus);
        verification.setVerifiedAt(Instant.now());
        verification.setVerifiedBy(admin);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            verification.setNotes(request.notes().trim());
        }
        ProfileVerification saved = profileVerificationRepository.save(verification);
        auditLogService.record(admin, auditAction, "profile_verification", saved.getId(), null);
        return toResponse(saved);
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminVerificationResponse toResponse(ProfileVerification verification) {
        return new AdminVerificationResponse(
                verification.getId(),
                verification.getProfile().getUser().getUuid(),
                verification.getVerificationStatus(),
                verification.getVerificationType(),
                verification.getVerifiedAt(),
                verification.getNotes());
    }
}
