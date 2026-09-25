package com.pavitraristaa.trust.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.trust.dto.AppealResponse;
import com.pavitraristaa.trust.dto.SubmitAppealRequest;
import com.pavitraristaa.trust.entity.Appeal;
import com.pavitraristaa.trust.entity.AppealStatus;
import com.pavitraristaa.trust.repository.AppealRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submission only, matching the contract exactly (POST /appeals is the only user-facing endpoint - no GET/list
 * for the submitter). Review and resolution is an admin action, built the same way moderation/reports/support
 * tickets were: see admin.service.AdminAppealService.
 */
@Service
public class AppealService {

    private final AuthService authService;
    private final AppealRepository appealRepository;

    public AppealService(AuthService authService, AppealRepository appealRepository) {
        this.authService = authService;
        this.appealRepository = appealRepository;
    }

    @Transactional
    public AppealResponse submit(AuthenticatedUser principal, SubmitAppealRequest request) {
        UserAccount self = authService.requireUsable(principal);
        Appeal appeal = new Appeal();
        appeal.setUuid(UUID.randomUUID());
        appeal.setUser(self);
        appeal.setType(request.type().trim());
        appeal.setDetails(request.details().trim());
        appeal.setStatus(AppealStatus.OPEN);
        appeal.setCreatedAt(Instant.now());
        return toResponse(appealRepository.save(appeal));
    }

    private AppealResponse toResponse(Appeal appeal) {
        return new AppealResponse(
                appeal.getUuid(), appeal.getType(), appeal.getDetails(), appeal.getStatus().name(),
                appeal.getResolutionNotes(), appeal.getResolvedAt(), appeal.getCreatedAt());
    }
}
