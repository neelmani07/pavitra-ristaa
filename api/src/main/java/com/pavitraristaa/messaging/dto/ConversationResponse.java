package com.pavitraristaa.messaging.dto;

import com.pavitraristaa.profile.dto.UserSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID matchId,
        String status,
        List<UserSummaryResponse> participants,
        Instant updatedAt
) {
}
