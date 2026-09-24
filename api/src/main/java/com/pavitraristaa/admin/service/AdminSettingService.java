package com.pavitraristaa.admin.service;

import com.pavitraristaa.admin.dto.AdminSettingResponse;
import com.pavitraristaa.admin.dto.UpdateSettingRequest;
import com.pavitraristaa.admin.entity.SettingValueType;
import com.pavitraristaa.admin.entity.SystemSetting;
import com.pavitraristaa.admin.repository.SystemSettingRepository;
import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple key/value settings store (system_setting). No seed data ships with this pass - the table starts empty
 * and admins create keys as they're needed; value_type is informational only (this layer stores/returns
 * everything as text and lets the caller interpret it, matching the column's own TEXT type).
 */
@Service
public class AdminSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public AdminSettingService(
            SystemSettingRepository systemSettingRepository, UserAccountRepository userAccountRepository, AuditLogService auditLogService) {
        this.systemSettingRepository = systemSettingRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AdminSettingResponse> list() {
        return systemSettingRepository.findAllByOrderBySettingKeyAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminSettingResponse upsert(AuthenticatedUser principal, String key, UpdateSettingRequest request) {
        if (key == null || key.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Setting key is required");
        }
        SystemSetting setting = systemSettingRepository.findBySettingKey(key.trim())
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(key.trim());
                    created.setValueType(SettingValueType.STRING);
                    created.setPublic(false);
                    return created;
                });
        setting.setSettingValue(request.value());
        UserAccount admin = actingAdmin(principal);
        setting.setUpdatedBy(admin);
        setting.setUpdatedAt(Instant.now());
        SystemSetting saved = systemSettingRepository.save(setting);
        auditLogService.record(admin, "SETTING_UPDATED", "system_setting", saved.getId(), Map.of("key", saved.getSettingKey()));
        return toResponse(saved);
    }

    private UserAccount actingAdmin(AuthenticatedUser principal) {
        return userAccountRepository.findByUuid(principal.uuid())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    private AdminSettingResponse toResponse(SystemSetting setting) {
        return new AdminSettingResponse(
                setting.getSettingKey(), setting.getSettingValue(), setting.getValueType().name(),
                setting.getDescription(), setting.isPublic(), setting.getUpdatedAt());
    }
}
