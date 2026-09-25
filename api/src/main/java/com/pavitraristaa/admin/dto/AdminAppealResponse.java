package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminAppealResponse(
        UUID id,
        UUID userId,
        String type,
        String details,
        String status,
        String resolutionNotes,
        UUID resolvedById,
        Instant resolvedAt,
        Instant createdAt
) {
}
