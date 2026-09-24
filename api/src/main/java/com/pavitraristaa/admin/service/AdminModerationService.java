package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminModerationResponse;
import com.pavitraristaa.admin.dto.CreateModerationRequest;
import com.pavitraristaa.admin.dto.ResolveModerationRequest;
import com.pavitraristaa.admin.entity.Moderation;
import com.pavitraristaa.admin.entity.ModerationAction;
import com.pavitraristaa.admin.entity.ModerationStatus;
import com.pavitraristaa.admin.repository.ModerationRepository;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Standalone moderation actions (not tied to a report) plus listing/resolving any moderation row. */
@Service
public class AdminModerationService {

    private final ModerationRepository moderationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public AdminModerationService(
            ModerationRepository moderationRepository, UserAccountRepository userAccountRepository, AuditLogService auditLogService) {
        this.moderationRepository = moderationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminModerationResponse> list(String status, Integer page, Integer size) {
        var pageable = PaginationSupport.pageable(page, size);
        Page<Moderation> result = status == null || status.isBlank()
                ? moderationRepository.findAllByOrderByCreatedAtDesc(pageable)
                : moderationRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        List<AdminModerationResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminModerationResponse getOne(UUID moderationId) {
        return toResponse(requireModeration(moderationId));
    }

    @Transactional
    public AdminModerationResponse create(AuthenticatedUser principal, CreateModerationRequest request) {
        UserAccount admin = actingAdmin(principal);
        UserAccount target = userAccountRepository.findByUuid(request.targetUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        Moderation moderation = new Moderation();
        moderation.setUuid(UUID.randomUUID());
        moderation.setTargetUser(target);
        moderation.setAction(parseAction(request.action()));
        moderation.setReason(request.reason());
        moderation.setStatus(ModerationStatus.OPEN);
        moderation.setModerator(admin);
        moderation.setCreatedAt(Instant.now());
        Moderation saved = moderationRepository.save(moderation);
        auditLogService.record(admin, "MODERATION_CREATED", "moderation", saved.getId(), Map.of("action", saved.getAction().name()));
        return toResponse(saved);
    }

    @Transactional
    public AdminModerationResponse resolve(AuthenticatedUser principal, UUID moderationId, ResolveModerationRequest request) {
        Moderation moderation = requireModeration(moderationId);
        if (moderation.getStatus() == ModerationStatus.RESOLVED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This moderation entry is already resolved");
        }
        moderation.setStatus(ModerationStatus.RESOLVED);
        moderation.setResolvedAt(Instant.now());
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            moderation.setReason(moderation.getReason() == null
                    ? request.notes().trim()
                    : moderation.getReason() + "\n---\nResolution: " + request.notes().trim());
        }
        Moderation saved = moderationRepository.save(moderation);
        UserAccount admin = actingAdmin(principal);
        auditLogService.record(admin, "MODERATION_RESOLVED", "moderation", saved.getId(), null);
        return toResponse(saved);
    }

    private ModerationStatus parseStatus(String status) {
        try {
            return ModerationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid moderation status", Map.of("status", status));
        }
    }

    private ModerationAction parseAction(String action) {
        try {
            return ModerationAction.valueOf(action.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid moderation action", Map.of("action", action));
        }
    }

    private Moderation requireModeration(UUID moderationId) {
        return moderationRepository.findByUuid(moderationId)
                .orElseThrow(() -> new ApiException(ErrorCode.MODERATION_NOT_FOUND, "Moderation entry not found"));
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminModerationResponse toResponse(Moderation moderation) {
        return new AdminModerationResponse(
                moderation.getUuid(),
                moderation.getTargetUser() == null ? null : moderation.getTargetUser().getUuid(),
                moderation.getTargetProfileId(),
                moderation.getTargetMediaId(),
                moderation.getTargetMessageId(),
                moderation.getAction().name(),
                moderation.getReason(),
                moderation.getStatus().name(),
                moderation.getModerator() == null ? null : moderation.getModerator().getUuid(),
                moderation.getCreatedAt(),
                moderation.getResolvedAt());
    }
}
