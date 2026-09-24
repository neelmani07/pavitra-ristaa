package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminModerationResponse(
        UUID id,
        UUID targetUserId,
        Long targetProfileId,
        Long targetMediaId,
        Long targetMessageId,
        String action,
        String reason,
        String status,
        UUID moderatorId,
        Instant createdAt,
        Instant resolvedAt
) {
}
