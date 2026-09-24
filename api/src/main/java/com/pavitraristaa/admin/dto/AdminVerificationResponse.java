package com.pavitraristaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminVerificationResponse(
        Long id,
        UUID userId,
        String verificationStatus,
        String verificationType,
        Instant verifiedAt,
        String notes
) {
}
