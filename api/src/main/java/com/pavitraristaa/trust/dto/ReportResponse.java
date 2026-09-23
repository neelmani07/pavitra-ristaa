package com.pavitraristaa.trust.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String status,
        UUID reportedUserId,
        UUID reportedMessageId,
        String reasonCode,
        String details,
        Instant createdAt
) {
}
