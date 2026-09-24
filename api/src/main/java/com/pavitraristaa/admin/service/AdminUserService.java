package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminUserResponse;
import com.pavitraristaa.admin.dto.LoginHistoryResponse;
import com.pavitraristaa.admin.dto.SuspendUserRequest;
import com.pavitraristaa.admin.dto.UpdateUserRolesRequest;
import com.pavitraristaa.admin.event.UserStatusChangedEvent;
import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.auth.entity.LoginHistory;
import com.pavitraristaa.auth.entity.Role;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.entity.UserRole;
import com.pavitraristaa.auth.repository.LoginHistoryRepository;
import com.pavitraristaa.auth.repository.RoleRepository;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.repository.UserRoleRepository;
import com.pavitraristaa.auth.service.SessionTokenService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-side user management: search/inspect, role reassignment, suspend/activate/delete and login history.
 * Every write here is also recorded to audit_log via AuditLogService.
 */
@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SessionTokenService sessionTokenService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminUserService(
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            LoginHistoryRepository loginHistoryRepository,
            SessionTokenService sessionTokenService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.sessionTokenService = sessionTokenService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PagedData<AdminUserResponse> search(String status, String search, Integer page, Integer size) {
        AccountStatus statusFilter = parseStatus(status);
        Specification<UserAccount> spec = combine(statusFilter, search);
        Page<UserAccount> result = userAccountRepository.findAll(spec, PaginationSupport.pageable(page, size));
        List<AdminUserResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getOne(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public AdminUserResponse updateRoles(AuthenticatedUser principal, UUID userId, UpdateUserRolesRequest request) {
        UserAccount target = requireUser(userId);
        List<String> codes = request.roleCodes().stream().map(code -> code.trim().toUpperCase(Locale.ROOT)).distinct().toList();
        List<Role> roles = roleRepository.findByCodeIn(codes);
        if (roles.size() != codes.size()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "One or more role codes are unknown", Map.of("roleCodes", codes));
        }
        UserAccount admin = actingAdmin(principal);
        userRoleRepository.deleteByUser(target);
        Instant now = Instant.now();
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(target);
            userRole.setRole(role);
            userRole.setAssignedAt(now);
            userRole.setAssignedBy(admin);
            userRoleRepository.save(userRole);
        }
        auditLogService.record(admin, "USER_ROLES_UPDATED", "user", target.getId(), Map.of("roleCodes", codes));
        return toResponse(target);
    }

    @Transactional
    public AdminUserResponse suspend(AuthenticatedUser principal, UUID userId, SuspendUserRequest request) {
        UserAccount target = requireUser(userId);
        if (target.getAccountStatus() == AccountStatus.DELETED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Account is already deleted");
        }
        target.setAccountStatus(AccountStatus.SUSPENDED);
        target.setUpdatedAt(Instant.now());
        userAccountRepository.save(target);
        sessionTokenService.revokeAll(target);
        UserAccount admin = actingAdmin(principal);
        auditLogService.record(admin, "USER_SUSPENDED", "user", target.getId(), Map.of("reason", request.reason()));
        eventPublisher.publishEvent(new UserStatusChangedEvent(target, AccountStatus.SUSPENDED.name()));
        return toResponse(target);
    }

    /** Admin override: lifts SUSPENDED (moderator action) or DEACTIVATED (self-service) alike. */
    @Transactional
    public AdminUserResponse activate(AuthenticatedUser principal, UUID userId) {
        UserAccount target = requireUser(userId);
        if (target.getAccountStatus() == AccountStatus.DELETED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A deleted account cannot be reactivated");
        }
        target.setAccountStatus(AccountStatus.ACTIVE);
        target.setUpdatedAt(Instant.now());
        userAccountRepository.save(target);
        UserAccount admin = actingAdmin(principal);
        auditLogService.record(admin, "USER_ACTIVATED", "user", target.getId(), null);
        eventPublisher.publishEvent(new UserStatusChangedEvent(target, AccountStatus.ACTIVE.name()));
        return toResponse(target);
    }

    @Transactional
    public void delete(AuthenticatedUser principal, UUID userId) {
        UserAccount target = requireUser(userId);
        Instant now = Instant.now();
        target.setAccountStatus(AccountStatus.DELETED);
        target.setDeleted(true);
        target.setDeletedAt(now);
        target.setUpdatedAt(now);
        userAccountRepository.save(target);
        sessionTokenService.revokeAll(target);
        UserAccount admin = actingAdmin(principal);
        auditLogService.record(admin, "USER_DELETED", "user", target.getId(), null);
    }

    @Transactional(readOnly = true)
    public PagedData<LoginHistoryResponse> loginHistory(UUID userId, Integer page, Integer size) {
        UserAccount target = requireUser(userId);
        Pageable pageable = PaginationSupport.pageable(page, size);
        Page<LoginHistory> result = loginHistoryRepository.findByUserOrderByLoginAtDesc(target, pageable);
        List<LoginHistoryResponse> items = result.getContent().stream()
                .map(entry -> new LoginHistoryResponse(
                        entry.getLoginAt(), entry.getLoginType().name(), entry.isSuccess(),
                        entry.getIpAddress(), entry.getUserAgent(), entry.getFailureReason()))
                .toList();
        return new PagedData<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private Specification<UserAccount> combine(AccountStatus status, String search) {
        Specification<UserAccount> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("accountStatus"), status));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("mobile")), pattern)));
        }
        return spec;
    }

    private AccountStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid account status", Map.of("status", status));
        }
    }

    private UserAccount requireUser(UUID userId) {
        return userAccountRepository.findByUuid(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminUserResponse toResponse(UserAccount user) {
        List<String> roles = sessionTokenService.rolesOf(user);
        return new AdminUserResponse(
                user.getUuid(), user.getEmail(), user.getMobile(), user.getAccountStatus().name(), roles,
                user.isEmailVerified(), user.isMobileVerified(), user.getFailedLoginAttempts(), user.getLockedUntil(),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
