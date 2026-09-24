package com.pavitraristaa.notifications.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.notifications.dto.NotificationSettingResponse;
import com.pavitraristaa.notifications.dto.NotificationSettingUpdate;
import com.pavitraristaa.notifications.dto.UpdateNotificationSettingsRequest;
import com.pavitraristaa.notifications.entity.NotificationSetting;
import com.pavitraristaa.notifications.entity.NotificationType;
import com.pavitraristaa.notifications.repository.NotificationSettingRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A user only gets a notification_setting row once they've actually changed a preference (matching the unique
 * (user_id, notification_type) constraint); GET synthesizes the table's own column defaults - in_app/push/email
 * enabled, sms disabled - for every NotificationType that doesn't have a row yet, so the response always lists
 * every type without needing to pre-seed rows on registration.
 */
@Service
public class NotificationSettingService {

    private final AuthService authService;
    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationSettingService(AuthService authService, NotificationSettingRepository notificationSettingRepository) {
        this.authService = authService;
        this.notificationSettingRepository = notificationSettingRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationSettingResponse> get(AuthenticatedUser principal) {
        UserAccount self = authService.requireUsable(principal);
        Map<NotificationType, NotificationSetting> existing = new EnumMap<>(NotificationType.class);
        notificationSettingRepository.findByUser(self).forEach(setting -> existing.put(setting.getNotificationType(), setting));

        return List.of(NotificationType.values()).stream()
                .map(type -> {
                    NotificationSetting setting = existing.get(type);
                    return setting == null
                            ? new NotificationSettingResponse(type.name(), true, true, true, false, null)
                            : toResponse(setting);
                })
                .toList();
    }

    @Transactional
    public List<NotificationSettingResponse> update(AuthenticatedUser principal, UpdateNotificationSettingsRequest request) {
        UserAccount self = authService.requireUsable(principal);
        Instant now = Instant.now();
        for (NotificationSettingUpdate item : request.settings()) {
            NotificationType type = parseType(item.notificationType());
            NotificationSetting setting = notificationSettingRepository.findByUserAndNotificationType(self, type)
                    .orElseGet(() -> {
                        NotificationSetting created = new NotificationSetting();
                        created.setUser(self);
                        created.setNotificationType(type);
                        return created;
                    });
            if (item.inAppEnabled() != null) {
                setting.setInAppEnabled(item.inAppEnabled());
            }
            if (item.pushEnabled() != null) {
                setting.setPushEnabled(item.pushEnabled());
            }
            if (item.emailEnabled() != null) {
                setting.setEmailEnabled(item.emailEnabled());
            }
            if (item.smsEnabled() != null) {
                setting.setSmsEnabled(item.smsEnabled());
            }
            setting.setUpdatedAt(now);
            notificationSettingRepository.save(setting);
        }
        return get(principal);
    }

    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid notification type", Map.of("notificationType", type));
        }
    }

    private NotificationSettingResponse toResponse(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.getNotificationType().name(), setting.isInAppEnabled(), setting.isPushEnabled(),
                setting.isEmailEnabled(), setting.isSmsEnabled(), setting.getUpdatedAt());
    }
}
