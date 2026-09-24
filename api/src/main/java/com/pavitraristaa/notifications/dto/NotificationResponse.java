package com.pavitraristaa.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        String referenceType,
        Long referenceId,
        boolean isRead,
        Instant readAt,
        Instant createdAt
) {
}
