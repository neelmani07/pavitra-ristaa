package com.pavitraristaa.trust.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.trust.dto.CreateReportRequest;
import com.pavitraristaa.trust.dto.ReportReasonResponse;
import com.pavitraristaa.trust.dto.ReportResponse;
import com.pavitraristaa.trust.entity.Report;
import com.pavitraristaa.trust.entity.ReportReason;
import com.pavitraristaa.trust.repository.ReportReasonRepository;
import com.pavitraristaa.trust.repository.ReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final AuthService authService;
    private final UserAccountRepository userAccountRepository;
    private final ReportReasonRepository reportReasonRepository;
    private final ReportRepository reportRepository;
    private final MessageLookup messageLookup;

    public ReportService(
            AuthService authService,
            UserAccountRepository userAccountRepository,
            ReportReasonRepository reportReasonRepository,
            ReportRepository reportRepository,
            MessageLookup messageLookup
    ) {
        this.authService = authService;
        this.userAccountRepository = userAccountRepository;
        this.reportReasonRepository = reportReasonRepository;
        this.reportRepository = reportRepository;
        this.messageLookup = messageLookup;
    }

    @Transactional(readOnly = true)
    public List<ReportReasonResponse> listReasons() {
        return reportReasonRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(reason -> new ReportReasonResponse(reason.getId(), reason.getCode(), reason.getName(), reason.getDescription()))
                .toList();
    }

    @Transactional
    public ReportResponse create(AuthenticatedUser principal, CreateReportRequest request) {
        return create(principal, request.reportedUserId(), request.reportedMessageId(), request.reasonId(), request.details());
    }

    /** Also used by messaging's report-from-chat flow, which already knows the message's internal id. */
    @Transactional
    public ReportResponse create(AuthenticatedUser principal, UUID reportedUserId, UUID reportedMessageId, Long reasonId, String details) {
        UserAccount self = authService.requireUsable(principal);
        ReportReason reason = reportReasonRepository.findByIdAndActiveTrue(reasonId)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Unknown report reason", Map.of("reasonId", reasonId)));

        UserAccount reportedUser = null;
        if (reportedUserId != null) {
            if (self.getUuid().equals(reportedUserId)) {
                throw new ApiException(ErrorCode.CANNOT_INTERACT_WITH_SELF, "You cannot report yourself");
            }
            reportedUser = userAccountRepository.findByUuid(reportedUserId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        }
        Long reportedMessageInternalId = reportedMessageId == null ? null
                : messageLookup.internalIdOf(reportedMessageId)
                        .orElseThrow(() -> new ApiException(ErrorCode.MESSAGE_NOT_FOUND, "Message not found"));

        Report report = new Report();
        report.setUuid(UUID.randomUUID());
        report.setReporter(self);
        report.setReportedUser(reportedUser);
        report.setReportedMessageId(reportedMessageInternalId);
        report.setReason(reason);
        report.setDetails(details == null || details.isBlank() ? null : details.trim());
        report.setCreatedAt(Instant.now());
        Report saved = reportRepository.save(report);

        return new ReportResponse(
                saved.getUuid(), saved.getStatus().name(), reportedUserId, reportedMessageId, reason.getCode(),
                saved.getDetails(), saved.getCreatedAt());
    }
}
