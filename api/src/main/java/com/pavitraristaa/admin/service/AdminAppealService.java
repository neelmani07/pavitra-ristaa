package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminAppealResponse;
import com.pavitraristaa.admin.dto.ResolveAppealRequest;
import com.pavitraristaa.admin.event.AppealResolvedEvent;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.trust.entity.Appeal;
import com.pavitraristaa.trust.entity.AppealStatus;
import com.pavitraristaa.trust.repository.AppealRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin side of the appeal lifecycle - the review counterpart to trust.AppealService.submit(). */
@Service
public class AdminAppealService {

    private final AppealRepository appealRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminAppealService(
            AppealRepository appealRepository, UserAccountRepository userAccountRepository,
            AuditLogService auditLogService, ApplicationEventPublisher eventPublisher) {
        this.appealRepository = appealRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminAppealResponse> list(String status, Integer page, Integer size) {
        var pageable = PaginationSupport.pageable(page, size);
        Page<Appeal> result = status == null || status.isBlank()
                ? appealRepository.findAllByOrderByCreatedAtDesc(pageable)
                : appealRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        List<AdminAppealResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminAppealResponse getOne(UUID appealId) {
        return toResponse(requireAppeal(appealId));
    }

    @Transactional
    public AdminAppealResponse resolve(AuthenticatedUser principal, UUID appealId, ResolveAppealRequest request) {
        Appeal appeal = requireAppeal(appealId);
        if (appeal.getStatus() != AppealStatus.OPEN) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This appeal has already been resolved");
        }
        AppealStatus newStatus = parseCloseStatus(request.status());
        UserAccount admin = actingAdmin(principal);

        appeal.setStatus(newStatus);
        appeal.setResolutionNotes(request.resolutionNotes());
        appeal.setResolvedBy(admin);
        appeal.setResolvedAt(Instant.now());
        Appeal saved = appealRepository.save(appeal);

        auditLogService.record(admin, "APPEAL_RESOLVED", "appeal", saved.getId(), Map.of("status", newStatus.name()));
        eventPublisher.publishEvent(new AppealResolvedEvent(saved.getUser(), newStatus == AppealStatus.APPROVED));
        return toResponse(saved);
    }

    private AppealStatus parseCloseStatus(String status) {
        AppealStatus parsed = parseStatus(status);
        if (parsed == AppealStatus.OPEN) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "status must be APPROVED or REJECTED", Map.of("status", status));
        }
        return parsed;
    }

    private AppealStatus parseStatus(String status) {
        try {
            return AppealStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid appeal status", Map.of("status", status));
        }
    }

    private Appeal requireAppeal(UUID appealId) {
        return appealRepository.findByUuid(appealId)
                .orElseThrow(() -> new ApiException(ErrorCode.APPEAL_NOT_FOUND, "Appeal not found"));
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminAppealResponse toResponse(Appeal appeal) {
        return new AdminAppealResponse(
                appeal.getUuid(),
                appeal.getUser().getUuid(),
                appeal.getType(),
                appeal.getDetails(),
                appeal.getStatus().name(),
                appeal.getResolutionNotes(),
                appeal.getResolvedBy() == null ? null : appeal.getResolvedBy().getUuid(),
                appeal.getResolvedAt(),
                appeal.getCreatedAt());
    }
}
