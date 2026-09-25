package com.pavitraristaa.trust.dto;

import java.time.Instant;
import java.util.UUID;

public record AppealResponse(
        UUID id,
        String type,
        String details,
        String status,
        String resolutionNotes,
        Instant resolvedAt,
        Instant createdAt
) {
}
