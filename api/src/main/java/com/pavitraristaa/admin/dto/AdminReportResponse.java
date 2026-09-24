package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminReportResponse(
        UUID id,
        String status,
        UUID reporterId,
        UUID reportedUserId,
        Long reportedMessageId,
        String reasonCode,
        String details,
        UUID resolvedById,
        Instant resolvedAt,
        Instant createdAt
) {
}
