package com.pavitraristaa.connections.dto;

import com.pavitraristaa.profile.dto.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record InterestResponse(
        UUID id,
        UserSummaryResponse sender,
        UserSummaryResponse receiver,
        String relationshipMode,
        String status,
        String message,
        Instant createdAt,
        Instant respondedAt
) {
}
