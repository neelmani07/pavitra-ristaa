package com.pavitraristaa.auth.dto;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String deviceName,
        String platform,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        boolean current
) {
}
