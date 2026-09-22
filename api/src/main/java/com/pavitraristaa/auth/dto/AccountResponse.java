package com.pavitraristaa.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        String mobile,
        String accountStatus,
        boolean emailVerified,
        boolean mobileVerified,
        List<String> roles,
        Instant createdAt
) {
}
