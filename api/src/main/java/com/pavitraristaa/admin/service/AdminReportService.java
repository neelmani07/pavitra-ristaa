package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminReportResponse;
import com.pavitraristaa.admin.dto.ResolveReportRequest;
import com.pavitraristaa.admin.entity.Moderation;
import com.pavitraristaa.admin.entity.ModerationAction;
import com.pavitraristaa.admin.entity.ModerationStatus;
import com.pavitraristaa.admin.event.ReportResolvedEvent;
import com.pavitraristaa.admin.repository.ModerationRepository;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.trust.entity.Report;
import com.pavitraristaa.trust.entity.ReportStatus;
import com.pavitraristaa.trust.repository.ReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin side of the report lifecycle. Resolving a report with a punitive moderationAction also opens a
 * Moderation row against the reported user (source_report_id = this report) in the same transaction, so a
 * resolved report that led to real action always has a corresponding moderation record.
 */
@Service
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final ModerationRepository moderationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminReportService(
            ReportRepository reportRepository,
            ModerationRepository moderationRepository,
            UserAccountRepository userAccountRepository,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reportRepository = reportRepository;
        this.moderationRepository = moderationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminReportResponse> list(String status, Integer page, Integer size) {
        var pageable = PaginationSupport.pageable(page, size);
        Page<Report> result = status == null || status.isBlank()
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        List<AdminReportResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminReportResponse getOne(UUID reportId) {
        return toResponse(requireReport(reportId));
    }

    @Transactional
    public AdminReportResponse resolve(AuthenticatedUser principal, UUID reportId, ResolveReportRequest request) {
        Report report = requireReport(reportId);
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This report has already been closed");
        }
        ReportStatus newStatus = parseCloseStatus(request.status());
        UserAccount admin = actingAdmin(principal);
        Instant now = Instant.now();

        report.setStatus(newStatus);
        report.setResolvedBy(admin);
        report.setResolvedAt(now);
        Report saved = reportRepository.save(report);

        if (request.moderationAction() != null && !request.moderationAction().isBlank()) {
            if (report.getReportedUser() == null) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Report has no reported user to take moderation action against");
            }
            ModerationAction action = parseAction(request.moderationAction());
            Moderation moderation = new Moderation();
            moderation.setUuid(UUID.randomUUID());
            moderation.setTargetUser(report.getReportedUser());
            moderation.setSourceReportId(saved.getId());
            moderation.setAction(action);
            moderation.setReason(request.moderationReason());
            moderation.setStatus(ModerationStatus.OPEN);
            moderation.setModerator(admin);
            moderation.setCreatedAt(now);
            moderationRepository.save(moderation);
            auditLogService.record(admin, "REPORT_RESOLVED_WITH_ACTION", "report", saved.getId(), Map.of("action", action.name()));
        } else {
            auditLogService.record(admin, "REPORT_RESOLVED", "report", saved.getId(), Map.of("status", newStatus.name()));
        }

        eventPublisher.publishEvent(new ReportResolvedEvent(saved.getReporter(), newStatus.name()));
        return toResponse(saved);
    }

    private ReportStatus parseCloseStatus(String status) {
        ReportStatus parsed = parseStatus(status);
        if (parsed != ReportStatus.RESOLVED && parsed != ReportStatus.DISMISSED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "status must be RESOLVED or DISMISSED", Map.of("status", status));
        }
        return parsed;
    }

    private ReportStatus parseStatus(String status) {
        try {
            return ReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid report status", Map.of("status", status));
        }
    }

    private ModerationAction parseAction(String action) {
        try {
            return ModerationAction.valueOf(action.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid moderation action", Map.of("action", action));
        }
    }

    private Report requireReport(UUID reportId) {
        return reportRepository.findByUuid(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND, "Report not found"));
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminReportResponse toResponse(Report report) {
        return new AdminReportResponse(
                report.getUuid(),
                report.getStatus().name(),
                report.getReporter().getUuid(),
                report.getReportedUser() == null ? null : report.getReportedUser().getUuid(),
                report.getReportedMessageId(),
                report.getReason().getCode(),
                report.getDetails(),
                report.getResolvedBy() == null ? null : report.getResolvedBy().getUuid(),
                report.getResolvedAt(),
                report.getCreatedAt());
    }
}
