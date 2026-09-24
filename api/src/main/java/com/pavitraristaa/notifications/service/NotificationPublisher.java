package com.pavitraristaa.notifications.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.notifications.entity.Notification;
import com.pavitraristaa.notifications.entity.NotificationType;
import com.pavitraristaa.notifications.repository.NotificationRepository;
import com.pavitraristaa.notifications.repository.NotificationSettingRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates in-app notification rows, respecting a user's per-type opt-out. Only the in_app_enabled flag is
 * honored here - push/email/SMS delivery columns already exist on notification_setting for later, but no
 * provider is wired up yet (same "the schema is ready, the integration is deferred" shape as OTP delivery).
 */
@Service
class NotificationPublisher {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    NotificationPublisher(NotificationRepository notificationRepository, NotificationSettingRepository notificationSettingRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationSettingRepository = notificationSettingRepository;
    }

    @Transactional
    void notify(UserAccount recipient, NotificationType type, String title, String body, String referenceType, Long referenceId) {
        boolean inAppEnabled = notificationSettingRepository.findByUserAndNotificationType(recipient, type)
                .map(setting -> setting.isInAppEnabled())
                .orElse(true);
        if (!inAppEnabled) {
            return;
        }
        Notification notification = new Notification();
        notification.setUuid(UUID.randomUUID());
        notification.setUser(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }
}
