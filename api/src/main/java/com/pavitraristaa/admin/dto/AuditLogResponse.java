package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        UUID actorId,
        String action,
        String entityType,
        Long entityId,
        String metadata,
        Instant createdAt
) {
}
