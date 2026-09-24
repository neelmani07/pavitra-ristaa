package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String mobile,
        String accountStatus,
        List<String> roles,
        boolean emailVerified,
        boolean mobileVerified,
        short failedLoginAttempts,
        Instant lockedUntil,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
