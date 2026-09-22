package com.pavitraristaa.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String accountStatus,
        boolean verificationRequired
) {
}
