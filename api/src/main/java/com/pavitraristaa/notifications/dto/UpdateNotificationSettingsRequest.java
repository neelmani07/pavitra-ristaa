package com.pavitraristaa.notifications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateNotificationSettingsRequest(@NotEmpty @Valid List<NotificationSettingUpdate> settings) {
}
