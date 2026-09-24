package com.pavitraristaa.notifications.dto;

import jakarta.validation.constraints.NotBlank;

/** One entry in an UpdateNotificationSettingsRequest. Unset boolean fields keep the row's current/default value. */
public record NotificationSettingUpdate(
        @NotBlank String notificationType,
        Boolean inAppEnabled,
        Boolean pushEnabled,
        Boolean emailEnabled,
        Boolean smsEnabled
) {
}
