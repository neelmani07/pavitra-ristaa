package com.pavitraristaa.notifications.dto;

import java.time.Instant;

public record NotificationSettingResponse(
        String notificationType,
        boolean inAppEnabled,
        boolean pushEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        Instant updatedAt
) {
}
