package com.pavitraristaa.connections.dto;

import com.pavitraristaa.profile.dto.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

/** {@code user} is always the other participant, from the caller's point of view - never the caller. */
public record MatchResponse(
        UUID id,
        UserSummaryResponse user,
        String relationshipMode,
        String status,
        Instant matchedAt
) {
}
