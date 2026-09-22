package com.pavitraristaa.auth.dto;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        String tokenType
) {
}
